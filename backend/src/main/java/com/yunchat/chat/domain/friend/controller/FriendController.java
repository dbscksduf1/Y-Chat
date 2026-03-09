package com.yunchat.chat.domain.friend.controller;

import com.yunchat.chat.domain.friend.dto.FriendResponse;
import com.yunchat.chat.domain.friend.entity.Friend;
import com.yunchat.chat.domain.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    // 친구 요청
    @PostMapping("/request")
    public String requestFriend(@RequestParam String friendEmail,
                                Authentication authentication) {

        String userEmail = authentication.getName();
        friendService.requestFriend(userEmail, friendEmail);
        return "친구 요청 완료";
    }

    // 친구 수락
    @PostMapping("/accept")
    public String acceptFriend(@RequestParam String friendEmail,
                               Authentication authentication) {

        String userEmail = authentication.getName();
        friendService.acceptFriend(userEmail, friendEmail);
        return "친구 수락 완료";
    }

    // 친구 목록
    @GetMapping
    public List<FriendResponse> getFriends(Authentication authentication) {

        String userEmail = authentication.getName();
        return friendService.getFriends(userEmail);
    }
    @GetMapping("/pending")
    public List<FriendResponse> getPendingRequests(Authentication authentication) {

        String userEmail = authentication.getName();
        return friendService.getPendingRequests(userEmail);
    }
}