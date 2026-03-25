package com.yunchat.chat.domain.chat.controller;

import com.yunchat.chat.domain.chat.dto.ChatMessageRequest;
import com.yunchat.chat.domain.chat.dto.ChatMessageResponse;
import com.yunchat.chat.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // 🔹 메시지 조회
    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursorId
    ) {
        return chatMessageService.getMessages(roomId, cursorId);
    }

    // 🔹 메시지 읽음 처리
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long roomId,
                                        Authentication authentication) {

        String username = authentication.getName();
        chatMessageService.markAsRead(roomId, username);
        return ResponseEntity.ok().build();
    }

    // 🔹 방 나가기
    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId,
                                       Authentication authentication) {

        String username = authentication.getName();
        chatMessageService.leaveRoom(roomId, username);
        return ResponseEntity.ok().build();
    }

    // 🔹 메시지 삭제
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId,
                                           Authentication authentication) {

        String username = authentication.getName();
        chatMessageService.deleteMessage(messageId, username);
        return ResponseEntity.ok().build();
    }
}