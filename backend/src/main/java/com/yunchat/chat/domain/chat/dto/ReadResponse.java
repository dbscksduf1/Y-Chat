package com.yunchat.chat.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReadResponse {

    private Long messageId;
    private Long roomId;
}