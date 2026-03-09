package com.yunchat.chat.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {

    private Long roomId;
    private String sender;
    private String content;
}