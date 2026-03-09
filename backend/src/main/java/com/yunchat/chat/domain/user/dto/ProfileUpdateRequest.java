package com.yunchat.chat.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {

    private String profileImageUrl;  // null 허용 (삭제 가능)
    private String statusMessage;    // null 허용
}