# Execution Flows

## 0. Full End-to-End System Request Architecture & Pipeline
**Trigger:** Client connects over WebSockets/SockJS, negotiates session identity, sends STOMP frames, passes profanity moderation, persists to MySQL, and scales across nodes via Redis.

```mermaid
graph TD
    subgraph Client Layer
        Browser[Client Web Browser / SockJS]
    end

    subgraph Network & Ingress Layer
        WS_Endpoint["/ws/stomp Handshake Endpoint"]
        HandshakeHandler["UserHandshakeHandler (Principal Assignment)"]
    end

    subgraph Spring Boot Application Layer
        InboundChannel["ClientInboundChannel"]
        Interceptor["StompModerationInterceptor"]
        TrieFilter["AbuseMasterFilter (Prefix Trie)"]
        Controller["ChatController (@MessageMapping)"]
    end

    subgraph Data & Messaging Infrastructure Layer
        Repo["ChatMessageRepository (Spring Data JPA)"]
        MySQL[("MySQL Database")]
        Broker["Simple / STOMP Message Broker"]
        RedisPubSub["Redis Pub/Sub Channel"]
    end

    Browser -->|1. HTTP Upgrade / WS Handshake| WS_Endpoint
    WS_Endpoint -->|2. Generate Random UUID Principal| HandshakeHandler
    HandshakeHandler -->|3. Establish STOMP Connection| InboundChannel
    
    Browser -->|4. Send STOMP Frame /app/*| InboundChannel
    InboundChannel -->|5. Pre-send Intercept| Interceptor
    Interceptor -->|6. Check & Mask Content| TrieFilter
    TrieFilter -->|7. Clean Byte Payload| Controller
    
    Controller -->|8. Persist Record| Repo
    Repo -->|9. SQL Insert| MySQL
    
    Controller -->|10a. Convert & Send| Broker
    Controller -->|10b. Publish Cluster Event| RedisPubSub
    
    Broker -->|11a. Deliver to Destination| Browser
    RedisPubSub -->|11b. Sync Cluster Nodes| Broker
```

### Full Request Pipeline Trace
1. **Connection & Authentication (Handshake):** Client initiates HTTP upgrade request to `/ws/stomp`. `UserHandshakeHandler.determineUser()` generates a unique session `Principal` (UUID username) and attaches it to the WebSocket session.
2. **Channel Ingress:** Client sends a STOMP frame to `/app/chat.private`, `/app/chat.room/{roomId}`, or `/app/chat.broadcast`. Frame enters Spring's `ClientInboundChannel`.
3. **Real-time Moderation Interception:** `StompModerationInterceptor.preSend()` catches the incoming frame before any controller mapping. `AbuseMasterFilter` checks text using a character Trie data structure and replaces abusive words with `***`.
4. **Controller Routing:** Modified clean frame moves to `@MessageMapping` handlers inside [ChatController](file:///Users/rkraj/Desktop/chat-message-app/src/main/java/com/example/chatmessageapp/controller/ChatController.java).
5. **Database Persistence:** `ChatMessageRepository.save()` translates the DTO to `ChatMessageEntity` and executes an INSERT into MySQL.
6. **Multi-Node Cluster Scaling & Broker Delivery:**
   - **Local Node:** Message is published to `/user/{username}/queue/messages` or `/topic/*` via `SimpMessagingTemplate`.
   - **Cluster Nodes:** `RedisPubSubService.publishToRedis()` broadcasts the payload across Redis Pub/Sub channels so other application server instances relay the message to their connected users.

---

## 1. 1-on-1 Direct Messaging (Private DM)
**Trigger:** Client sends STOMP frame to `/app/chat.private` with JSON body `{ "sender": "alice", "recipient": "bob", "content": "..." }`


```mermaid
sequenceDiagram
    autonumber
    actor Alice as Sender (Alice)
    participant Interceptor as StompModerationInterceptor
    participant Filter as AbuseMasterFilter (Trie)
    participant Controller as ChatController
    participant Repo as ChatMessageRepository
    participant Broker as SimpMessagingTemplate
    actor Bob as Recipient (Bob)

    Alice->>Interceptor: STOMP SEND /app/chat.private
    Interceptor->>Filter: sanitize(content)
    Filter-->>Interceptor: Sanitized Payload (Masked if abusive)
    Interceptor->>Controller: sendPrivateMessage(ChatMessage)
    Controller->>Repo: save(ChatMessageEntity)
    Repo-->>Controller: Persisted Entity
    Controller->>Broker: convertAndSendToUser("bob", "/queue/messages", payload)
    Broker-->>Bob: Deliver to /user/bob/queue/messages
```

**Trace:**
1. → `StompModerationInterceptor.preSend()` catches inbound STOMP `SEND` frame
2. → `AbuseMasterFilter.sanitize()` checks content using Trie algorithm and masks banned words with asterisks if present
3. → Modified/clean frame passes to `@MessageMapping("/chat.private")` in `ChatController.sendPrivateMessage()`
4. → `chatMessageRepository.save()` persists the message entity to MySQL database
5. → `messagingTemplate.convertAndSendToUser("bob", "/queue/messages", chatMessage)` routes message to recipient session

**Final effect:** Recipient ("bob") receives private message on `/user/bob/queue/messages`; message stored in DB.  
**Gotchas:** `UserHandshakeHandler` assigns a Principal username to each WebSocket session upon connect so `/user/{username}/queue/messages` routes correctly.

---

## 2. Group Room Messaging
**Trigger:** Client sends STOMP frame to `/app/chat.room/{roomId}` with JSON payload

```mermaid
sequenceDiagram
    autonumber
    actor Client as User in Room
    participant Interceptor as StompModerationInterceptor
    participant Controller as ChatController
    participant Repo as ChatMessageRepository
    participant Broker as STOMP / Redis Broker
    actor RoomSubscribers as Room Members (/topic/room.123)

    Client->>Interceptor: STOMP SEND /app/chat.room/123
    Interceptor->>Controller: sendRoomMessage(roomId=123, ChatMessage)
    Controller->>Repo: save(ChatMessageEntity)
    Controller->>Broker: @SendTo("/topic/room.123")
    Broker-->>RoomSubscribers: Broadcast to all members subscribed to room 123
```

**Trace:**
1. → `StompModerationInterceptor.preSend()` intercepts and sanitizes payload
2. → `ChatController.sendRoomMessage()` invoked with `@DestinationVariable String roomId`
3. → Room ID set on message object; `chatMessageRepository.save()` persists to MySQL
4. → `@SendTo("/topic/room.{roomId}")` broadcasts payload to simple broker / Redis pub-sub

**Final effect:** All clients subscribed to `/topic/room.{roomId}` receive real-time message.  
**Gotchas:** Redis Pub/Sub relay ensures multi-instance server synchronization for room topics across cluster nodes.

---

## 3. Broadcast Channel Messaging
**Trigger:** Client sends STOMP frame to `/app/chat.broadcast`

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Announcer
    participant Interceptor as StompModerationInterceptor
    participant Controller as ChatController
    participant Repo as ChatMessageRepository
    participant Broker as STOMP Broker
    actor AllUsers as All Connected Users

    Admin->>Interceptor: STOMP SEND /app/chat.broadcast
    Interceptor->>Controller: broadcastMessage(ChatMessage)
    Controller->>Repo: save(ChatMessageEntity)
    Controller->>Broker: @SendTo("/topic/broadcast")
    Broker-->>AllUsers: Broadcast message to /topic/broadcast
```

**Trace:**
1. → `StompModerationInterceptor.preSend()` validates and masks profane text
2. → `ChatController.broadcastMessage()` invoked
3. → `chatMessageRepository.save()` persists broadcast entry to MySQL
4. → `@SendTo("/topic/broadcast")` broadcasts message to all subscribers

**Final effect:** Every connected user subscribed to `/topic/broadcast` receives announcement.  
**Gotchas:** Heavy broadcast channels should rely on Redis/External broker in scaled environments.

---

## 4. Real-Time Moderation Interception (AbuseMasterFilter)
**Trigger:** Inbound STOMP `SEND` frame contains profane text matching prefix Trie dictionary

```mermaid
flowchart TD
    A[Inbound STOMP Frame] --> B{Command == SEND?}
    B -- No --> C[Pass Message Unchanged]
    B -- Yes --> D[Extract Byte Payload & Deserialize JSON]
    D --> E[AbuseMasterFilter.sanitize]
    E --> F{Contains Banned Word?}
    F -- No --> G[Forward Original Message]
    F -- Yes --> H[Mask Banned Words with ***]
    H --> I[Set chatMessage.masked = true]
    I --> J[Serialize Back to Byte Payload]
    J --> K[Rebuild Message & Pass Clean Frame Downstream]
```

**Trace:**
1. → Inbound frame captured in `StompModerationInterceptor.preSend()`
2. → Payload deserialized to `ChatMessage`
3. → `AbuseMasterFilter` performs Aho-Corasick / Trie search over character array
4. → Profane tokens replaced with `***` and `chatMessage.setMasked(true)` set
5. → Clean payload re-serialized to byte array and returned via `MessageBuilder`

**Final effect:** Downstream controllers, DB persistence, and recipient subscriptions only receive sanitized clean content.  
**Gotchas:** Interception happens before controller mapping; harmful messages are purged at the ingress edge.


