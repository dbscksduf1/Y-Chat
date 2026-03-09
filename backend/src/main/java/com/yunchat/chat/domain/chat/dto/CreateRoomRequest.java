package com.yunchat.chat.domain.chat.dto;

import com.yunchat.chat.domain.chat.entity.RoomType;
import lombok.Getter;

@Getter
public class CreateRoomRequest {

    private String roomName;
    private RoomType roomType;
}