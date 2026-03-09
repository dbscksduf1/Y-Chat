package com.yunchat.chat.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomName;

    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    private LocalDateTime createdAt;

    public ChatRoom(String roomName, RoomType roomType) {
        this.roomName = roomName;
        this.roomType = roomType;
        this.createdAt = LocalDateTime.now();
    }
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}