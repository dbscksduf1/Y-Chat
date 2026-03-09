package com.yunchat.chat.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room not found"),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "You are not a member of this room"),
    USER_ALREADY_IN_ROOM(HttpStatus.CONFLICT, "User already in the room"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Message not found"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),

    // 🔥 추가
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}