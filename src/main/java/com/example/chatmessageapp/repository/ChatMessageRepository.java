package com.example.chatmessageapp.repository;

import com.example.chatmessageapp.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByRoomIdOrderByTimestampAsc(String roomId);

    List<ChatMessageEntity> findBySenderAndRecipientOrSenderAndRecipientOrderByTimestampAsc(
            String sender1, String recipient1, String sender2, String recipient2);
}

