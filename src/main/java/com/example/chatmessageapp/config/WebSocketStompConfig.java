package com.example.chatmessageapp.config;

import com.example.chatmessageapp.handler.RawWebSocketHandler;
import com.example.chatmessageapp.interceptor.StompModerationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Full WebSocket Configuration support:
 * 1. Raw WebSockets on /ws/echo (Phase 1)
 * 2. STOMP WebSocket Messaging on /ws/stomp (Phase 2 & 3)
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketStompConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final RawWebSocketHandler rawWebSocketHandler;
    private final StompModerationInterceptor stompModerationInterceptor;

    public WebSocketStompConfig(RawWebSocketHandler rawWebSocketHandler,
                                StompModerationInterceptor stompModerationInterceptor) {
        this.rawWebSocketHandler = rawWebSocketHandler;
        this.stompModerationInterceptor = stompModerationInterceptor;
    }

    // Phase 1 Raw WebSocket handler registration
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(rawWebSocketHandler, "/ws/echo")
                .setAllowedOrigins("*");
    }

    // Phase 2 STOMP broker registration
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint over standard WebSockets and SockJS fallback with custom handshake handler
        registry.addEndpoint("/ws/stomp")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();

        registry.addEndpoint("/ws/stomp")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new UserHandshakeHandler());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destination prefix for client-to-server messages (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");

        // Destination prefixes for server-to-client broadcast/pubsub topics
        // /topic -> group chat & broadcast channels
        // /queue -> 1-on-1 direct user messaging
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Target destination for user-specific 1-on-1 messaging (/user/queue/messages)
        registry.setUserDestinationPrefix("/user");
    }

    // Phase 3 Inbound channel interceptor for Abuse Master moderation
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompModerationInterceptor);
    }
}
