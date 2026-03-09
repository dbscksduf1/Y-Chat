package com.yunchat.chat.domain.user.service;

import com.yunchat.chat.domain.user.entity.Block;
import com.yunchat.chat.domain.user.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;

    public void blockUser(String blocker, String target) {

        if (blockRepository.existsByBlockerUsernameAndBlockedUsernameOrBlockerUsernameAndBlockedUsername(
                blocker, target,
                target, blocker
        )) {
            return;
        }

        blockRepository.save(new Block(blocker, target));
    }

    public void unblockUser(String blocker, String target) {

        blockRepository.findByBlockerUsernameAndBlockedUsername(blocker, target)
                .ifPresent(blockRepository::delete);

    }

    public List<Block> getBlockedUsers(String blocker) {

        return blockRepository.findByBlockerUsername(blocker);

    }

    public boolean isBlocked(String user1, String user2) {

        return blockRepository.existsByBlockerUsernameAndBlockedUsernameOrBlockerUsernameAndBlockedUsername(
                user1, user2,
                user2, user1
        );

    }
}