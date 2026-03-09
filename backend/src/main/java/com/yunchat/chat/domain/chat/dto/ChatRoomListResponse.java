package com.yunchat.chat.domain.chat.dto;

import java.time.LocalDateTime;

public class ChatRoomListResponse {

    private Long roomId;
    private String roomName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;

    public ChatRoomListResponse(Long roomId,
                                String roomName,
                                String lastMessage,
                                LocalDateTime lastMessageTime,
                                Long unreadCount) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }
}