package com.yunchat.chat.domain.user.controller;

import com.yunchat.chat.domain.user.entity.Block;
import com.yunchat.chat.domain.user.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/block")
    public String block(@RequestParam String username,
                        Authentication authentication) {

        String currentUser = authentication.getName();
        blockService.blockUser(currentUser, username);

        return "User blocked successfully";
    }

    @DeleteMapping("/block")
    public String unblock(@RequestParam String username,
                          Authentication authentication) {

        String currentUser = authentication.getName();
        blockService.unblockUser(currentUser, username);

        return "User unblocked successfully";
    }

    @GetMapping("/blocks")
    public List<Block> getBlocks(Authentication authentication) {

        String currentUser = authentication.getName();
        return blockService.getBlockedUsers(currentUser);

    }
}