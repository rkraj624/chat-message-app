package com.example.chatmessageapp.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket handler for Phase 1.
 * Manages active WebSocket sessions in-memory and echoes received text messages.
 */
@Component
public class RawWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocketHandler.class);

    // Thread-safe collection to track all connected WebSocket sessions
    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        activeSessions.add(session);
        log.info("[WebSocket Established] Session ID: {}, Remote Address: {}", 
                session.getId(), session.getRemoteAddress());

        // Send a welcome greeting frame to the client upon handshake completion
        session.sendMessage(new TextMessage("Connected to raw WebSocket server! Your session ID is: " + session.getId()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("[Message Received] From Session ID {}: {}", session.getId(), payload);

        // Echo the message back to the sender
        String responsePayload = "Echo: " + payload;
        session.sendMessage(new TextMessage(responsePayload));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        activeSessions.remove(session);
        log.info("[WebSocket Closed] Session ID: {}, Close Status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WebSocket Error] Session ID: {}", session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        activeSessions.remove(session);
    }

    public Set<WebSocketSession> getActiveSessions() {
        return activeSessions;
    }
}
