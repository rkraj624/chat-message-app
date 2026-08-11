package com.example.chatmessageapp.entity;

import com.example.chatmessageapp.model.ChatMessage;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChatMessage.MessageType type;

    private String sender;
    private String recipient;
    private String roomId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private long timestamp;
    private boolean masked;

    public ChatMessageEntity() {
    }

    public ChatMessageEntity(ChatMessage.MessageType type, String sender, String recipient, String roomId, String content, long timestamp, boolean masked) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.roomId = roomId;
        this.content = content;
        this.timestamp = timestamp;
        this.masked = masked;
    }

    public static ChatMessageEntity fromModel(ChatMessage model) {
        return new ChatMessageEntity(
                model.getType(),
                model.getSender(),
                model.getRecipient(),
                model.getRoomId(),
                model.getContent(),
                model.getTimestamp() == 0 ? Instant.now().toEpochMilli() : model.getTimestamp(),
                model.isMasked()
        );
    }

    public Long getId() {
        return id;
    }

    public ChatMessage.MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isMasked() {
        return masked;
    }
}
