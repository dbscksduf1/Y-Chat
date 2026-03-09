package com.yunchat.chat.domain.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String statusMessage;
    private boolean online;
    private String lastSeen;

}