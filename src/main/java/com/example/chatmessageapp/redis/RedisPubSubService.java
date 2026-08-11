package com.example.chatmessageapp.redis;

import com.example.chatmessageapp.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service to manage horizontal scaling across multiple Spring Boot nodes using Redis Pub/Sub.
 */
@Service
public class RedisPubSubService implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisPubSubService(RedisTemplate<String, Object> redisTemplate,
                              SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Publishes a ChatMessage to Redis channel for multi-node distribution.
     */
    public void publishToRedis(String channel, ChatMessage chatMessage) {
        try {
            String json = objectMapper.writeValueAsString(chatMessage);
            redisTemplate.convertAndSend(channel, json);
            log.info("[Redis Pub] Published to channel '{}': {}", channel, chatMessage.getContent());
        } catch (Exception e) {
            log.error("Failed to publish message to Redis channel {}", channel, e);
        }
    }

    /**
     * Listens for messages published across Redis cluster from OTHER Spring Boot instances.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessage chatMessage = objectMapper.readValue(body, ChatMessage.class);

            log.info("[Redis Sub] Received from Redis channel '{}': {}", channel, chatMessage.getContent());

            // Relay to local STOMP subscribers on this server instance
            if (chatMessage.getRecipient() != null) {
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getRecipient(),
                        "/queue/messages",
                        chatMessage
                );
            } else if (chatMessage.getRoomId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/room." + chatMessage.getRoomId(),
                        chatMessage
                );
            } else {
                messagingTemplate.convertAndSend(
                        "/topic/broadcast",
                        chatMessage
                );
            }
        } catch (Exception e) {
            log.error("Error processing Redis PubSub message", e);
        }
    }
}
