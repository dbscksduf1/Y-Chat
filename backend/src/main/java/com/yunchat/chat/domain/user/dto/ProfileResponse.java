package com.yunchat.chat.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileResponse {

    private String nickname;
    private String profileImageUrl;
    private String statusMessage;
    private LocalDateTime lastActiveAt;
    private boolean online;
}