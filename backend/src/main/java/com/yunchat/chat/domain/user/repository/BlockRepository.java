package com.yunchat.chat.domain.user.repository;

import com.yunchat.chat.domain.user.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerUsernameAndBlockedUsername(String blocker, String blocked);

    boolean existsByBlockerUsernameAndBlockedUsername(String blocker, String blocked);

    boolean existsByBlockerUsernameAndBlockedUsernameOrBlockerUsernameAndBlockedUsername(
            String blocker1, String blocked1,
            String blocker2, String blocked2
    );

    List<Block> findByBlockerUsername(String blocker);
}