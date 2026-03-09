package com.yunchat.chat.domain.friend.repository;

import com.yunchat.chat.domain.friend.entity.Friend;
import com.yunchat.chat.domain.friend.entity.FriendStatus;
import com.yunchat.chat.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    Optional<Friend> findByUserAndFriend(User user, User friend);

    List<Friend> findByUserAndStatus(User user, FriendStatus status);

    List<Friend> findByFriendAndStatus(User friend, FriendStatus status);
}