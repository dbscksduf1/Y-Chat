package com.yunchat.chat.domain.chat.repository;

import com.yunchat.chat.domain.chat.entity.ChatMessage;
import com.yunchat.chat.domain.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long>{

    Page<ChatMessage> findByRoomAndDeletedFalse(
            ChatRoom room,
            Pageable pageable
    );


    // unread 메시지 조회 (내가 보낸 메시지 제외)
    @Query("""
        select m
        from ChatMessage m
        where m.room.id = :roomId
        and m.deleted = false
        and m.sender != :email
        and not exists (
            select 1 from MessageRead r
            where r.message = m
            and r.username = :email
        )
    """)
    List<ChatMessage> findUnreadMessages(Long roomId, String email);

    // unreadCount 집계 (내가 보낸 메시지 제외)
    @Query("""
        select m.room.id, count(m)
        from ChatMessage m
        where m.deleted = false
        and m.sender != :email
        and not exists (
            select 1 from MessageRead r
            where r.message = m
            and r.username = :email
        )
        group by m.room.id
    """)
    List<Object[]> countUnreadGroupedByRoom(String email);
}