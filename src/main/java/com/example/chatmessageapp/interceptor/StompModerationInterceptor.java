package com.example.chatmessageapp.interceptor;

import com.example.chatmessageapp.model.ChatMessage;
import com.example.chatmessageapp.moderation.AbuseMasterFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Inbound STOMP Channel Interceptor.
 * Intercepts incoming messages BEFORE they reach controllers or broker topics,
 * enforcing real-time profanity moderation.
 */
@Component
public class StompModerationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompModerationInterceptor.class);
    private final AbuseMasterFilter abuseMasterFilter;
    private final ObjectMapper objectMapper;

    public StompModerationInterceptor(AbuseMasterFilter abuseMasterFilter) {
        this.abuseMasterFilter = abuseMasterFilter;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Only inspect SEND commands (chat payload frames)
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Object payloadObj = message.getPayload();
            if (payloadObj instanceof byte[] bytes) {
                try {
                    String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                    ChatMessage chatMessage = objectMapper.readValue(jsonStr, ChatMessage.class);

                    if (chatMessage.getContent() != null) {
                        AbuseMasterFilter.ModerationResult result = abuseMasterFilter.sanitize(chatMessage.getContent());

                        if (result.wasMasked()) {
                            log.warn("[ABUSE MASTER INTERCEPTED] Sender '{}' attempted abusive content: '{}'", 
                                    chatMessage.getSender(), chatMessage.getContent());
                            chatMessage.setContent(result.sanitizedContent());
                            chatMessage.setMasked(true);

                            // Re-serialize modified clean payload
                            byte[] cleanBytes = objectMapper.writeValueAsBytes(chatMessage);
                            return MessageBuilder.createMessage(cleanBytes, accessor.getMessageHeaders());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse STOMP message payload in interceptor", e);
                }
            }
        }
        return message;
    }
}
