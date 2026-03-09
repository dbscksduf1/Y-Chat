package com.yunchat.chat.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String blockerUsername;

    private String blockedUsername;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected Block() {}

    public Block(String blockerUsername, String blockedUsername) {
        this.blockerUsername = blockerUsername;
        this.blockedUsername = blockedUsername;
    }

    public String getBlockerUsername() {
        return blockerUsername;
    }

    public String getBlockedUsername() {
        return blockedUsername;
    }
}