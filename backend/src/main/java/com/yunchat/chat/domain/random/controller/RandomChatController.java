package com.yunchat.chat.domain.random.controller;

import com.yunchat.chat.domain.random.dto.RandomMatchResponse;
import com.yunchat.chat.domain.random.service.RandomChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/random")
public class RandomChatController {

    private final RandomChatService randomChatService;

    @PostMapping("/enter")
    public RandomMatchResponse enter(Principal principal) {

        String email = principal.getName();

        return randomChatService.enter(email);
    }

    @PostMapping("/cancel")
    public void cancel(Principal principal) {

        String email = principal.getName();

        randomChatService.cancel(email);
    }

    @PostMapping("/leave/{roomId}")
    public void leave(@PathVariable Long roomId) {

        randomChatService.leave(roomId);
    }
}