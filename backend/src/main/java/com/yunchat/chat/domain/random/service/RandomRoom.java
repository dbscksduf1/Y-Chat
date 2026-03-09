package com.yunchat.chat.domain.random.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RandomRoom {

    private Long roomId;
    private String userA;
    private String userB;
    private boolean active;
}