package com.yunchat.chat.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrivateMessageRequest {

    private String sender;
    private String receiver;
    private String message;
}