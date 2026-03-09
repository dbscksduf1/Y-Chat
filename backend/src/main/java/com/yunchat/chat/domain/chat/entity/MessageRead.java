package com.yunchat.chat.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class MessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    private String username;

    private LocalDateTime readAt;

    public MessageRead(ChatMessage message, String username) {
        this.message = message;
        this.username = username;
        this.readAt = LocalDateTime.now();
    }
}