package com.yunchat.chat.domain.chat.repository;

import com.yunchat.chat.domain.chat.entity.ChatMessage;
import com.yunchat.chat.domain.chat.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageReadRepository
        extends JpaRepository<MessageRead, Long> {

    boolean existsByMessageAndUsername(ChatMessage message, String username);

    @Query("""
        SELECT COUNT(cm)
        FROM ChatMessage cm
        WHERE cm.room.id = :roomId
          AND cm.deleted = false
          AND cm.sender <> :username
          AND cm NOT IN (
              SELECT mr.message
              FROM MessageRead mr
              WHERE mr.username = :username
          )
    """)
    Long countUnreadMessages(Long roomId, String username);
}