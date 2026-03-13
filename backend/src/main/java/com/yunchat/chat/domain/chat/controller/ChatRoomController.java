package com.yunchat.chat.domain.chat.controller;

import com.yunchat.chat.domain.chat.dto.ChatRoomListResponse;
import com.yunchat.chat.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatMessageService chatMessageService;

    // 🔹 내 채팅방 조회
    @GetMapping
    public List<ChatRoomListResponse> getMyRooms(Authentication authentication) {
        String currentUserEmail = authentication.getName();
        return chatMessageService.getMyChatRooms(currentUserEmail);
    }

    // 🔹 채팅방 생성
    @PostMapping
    public String createRoom(@RequestParam String targetUsername,
                             Authentication authentication) {

        String currentUserEmail = authentication.getName();
        chatMessageService.createPrivateRoom(currentUserEmail, targetUsername);

        return "Room created successfully";
    }

    // 🔹 채팅방 초대
    @PostMapping("/{roomId}/invite")
    public String inviteUser(@PathVariable Long roomId,
                             @RequestParam String username,
                             Authentication authentication) {

        String currentUserEmail = authentication.getName();
        chatMessageService.inviteMember(roomId, currentUserEmail, username);

        return "Member invited successfully";
    }

    // 🔹 1:1 채팅방 생성
    @PostMapping("/private")
    public Long createPrivateRoom(
            @RequestParam String friendUsername,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return chatMessageService.createPrivateRoom(username, friendUsername);
    }
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}