package com.example.chatmessageapp.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Custom HandshakeHandler that assigns a Principal based on the query parameter 'username'
 * or URL parameters during STOMP WebSocket handshake.
 */
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String uri = request.getURI().toString();
        String username = "Anonymous";

        if (uri.contains("username=")) {
            username = uri.substring(uri.indexOf("username=") + 9);
            if (username.contains("&")) {
                username = username.substring(0, username.indexOf("&"));
            }
        }

        final String finalUsername = username;
        return () -> finalUsername;
    }
}
