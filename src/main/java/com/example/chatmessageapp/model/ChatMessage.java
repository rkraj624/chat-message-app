package com.example.chatmessageapp.model;

import java.time.Instant;

public class ChatMessage {

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        TYPING
    }

    private String id;
    private MessageType type;
    private String sender;
    private String recipient; // null for group chat
    private String roomId;    // null for 1-on-1 direct message
    private String content;
    private long timestamp;
    private boolean masked;

    public ChatMessage() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public ChatMessage(MessageType type, String sender, String recipient, String roomId, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.roomId = roomId;
        this.content = content;
        this.timestamp = Instant.now().toEpochMilli();
        this.masked = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }
}
