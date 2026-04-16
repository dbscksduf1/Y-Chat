package com.yunchat.chat.domain.chat.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatMessageResponse implements Serializable {

    private Long id;
    private Long roomId;
    private String sender;
    private String content;
    private LocalDateTime createdAt;
    private String profileImageUrl;
    private int unreadCount;

    // 기본 생성자 (Jackson deserialize용)
    public ChatMessageResponse() {
    }

    public ChatMessageResponse(Long id,
                               Long roomId,
                               String sender,
                               String content,
                               LocalDateTime createdAt,
                               String profileImageUrl,
                               int unreadCount) {
        this.id = id;
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.createdAt = createdAt;
        this.profileImageUrl = profileImageUrl;
        this.unreadCount = unreadCount;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    // setter (Jackson deserialize용)

    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}