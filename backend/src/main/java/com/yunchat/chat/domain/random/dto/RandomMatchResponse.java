package com.yunchat.chat.domain.random.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RandomMatchResponse {

    private String status;
    private Long roomId;
}