package com.example.chatmessageapp.controller;

import com.example.chatmessageapp.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Controller for handling STOMP messages across all messaging scopes:
 * 1. 1-on-1 Direct Messages
 * 2. Multi-participant Group Rooms
 * 3. Broadcast Channels
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final com.example.chatmessageapp.repository.ChatMessageRepository chatMessageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          com.example.chatmessageapp.repository.ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * 1-on-1 Direct Messaging (Instagram DM style)
     * Client sends to: /app/chat.private
     * Recipient listens on: /user/{username}/queue/messages
     */
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        log.info("[1-on-1 DM] Sender: '{}' -> Recipient: '{}': Content: '{}'",
                chatMessage.getSender(), chatMessage.getRecipient(), chatMessage.getContent());

        // Persist to MySQL database
        chatMessageRepository.save(com.example.chatmessageapp.entity.ChatMessageEntity.fromModel(chatMessage));

        // Send to specific recipient session/queue
        messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipient(),
                "/queue/messages",
                chatMessage
        );
    }

    /**
     * Group Chat Room Messaging (Discord / WhatsApp Group style)
     * Client sends to: /app/chat.room/{roomId}
     * Room members subscribe to: /topic/room.{roomId}
     */
    @MessageMapping("/chat.room/{roomId}")
    @SendTo("/topic/room.{roomId}")
    public ChatMessage sendRoomMessage(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
        log.info("[Group Room {}] Sender: '{}': Content: '{}'",
                roomId, chatMessage.getSender(), chatMessage.getContent());
        chatMessage.setRoomId(roomId);

        // Persist to MySQL database
        chatMessageRepository.save(com.example.chatmessageapp.entity.ChatMessageEntity.fromModel(chatMessage));

        return chatMessage;
    }

    /**
     * One-to-Many Broadcast Channel (Announcement style)
     * Client sends to: /app/chat.broadcast
     * All subscribers listen on: /topic/broadcast
     */
    @MessageMapping("/chat.broadcast")
    @SendTo("/topic/broadcast")
    public ChatMessage broadcastMessage(@Payload ChatMessage chatMessage) {
        log.info("[Broadcast Channel] Sender: '{}': Content: '{}'",
                chatMessage.getSender(), chatMessage.getContent());

        // Persist to MySQL database
        chatMessageRepository.save(com.example.chatmessageapp.entity.ChatMessageEntity.fromModel(chatMessage));

        return chatMessage;
    }
}
