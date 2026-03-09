package com.yunchat.chat.domain.friend.service;

import com.yunchat.chat.domain.friend.dto.FriendResponse;
import com.yunchat.chat.domain.friend.entity.Friend;
import com.yunchat.chat.domain.friend.entity.FriendStatus;
import com.yunchat.chat.domain.friend.repository.FriendRepository;
import com.yunchat.chat.domain.user.entity.User;
import com.yunchat.chat.domain.user.repository.UserRepository;
import com.yunchat.chat.domain.user.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yunchat.chat.global.websocket.OnlineUserService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final BlockService blockService;
    private final OnlineUserService onlineUserService;

    public void requestFriend(String userEmail, String friendEmail) {

        if (userEmail.equals(friendEmail)) {
            throw new IllegalArgumentException("자기 자신에게 요청 불가");
        }

        if (blockService.isBlocked(userEmail, friendEmail)) {
            throw new IllegalArgumentException("차단된 사용자입니다");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new IllegalArgumentException("상대 없음"));

        if (friendRepository.findByUserAndFriend(user, friend).isPresent()) {
            throw new IllegalArgumentException("이미 요청함");
        }

        Friend request = new Friend(user, friend);
        friendRepository.save(request);
    }

    public void acceptFriend(String userEmail, String friendEmail) {

        if (blockService.isBlocked(userEmail, friendEmail)) {
            throw new IllegalArgumentException("차단된 사용자입니다");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new IllegalArgumentException("상대 없음"));

        Friend request = friendRepository
                .findByUserAndFriend(friend, user)
                .orElseThrow(() -> new IllegalArgumentException("요청 없음"));

        request.accept();

        Friend reverse = friendRepository
                .findByUserAndFriend(user, friend)
                .orElseGet(() -> friendRepository.save(new Friend(user, friend)));

        reverse.accept();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return friendRepository.findByUserAndStatus(user, FriendStatus.ACCEPTED)
                .stream()
                .filter(friend -> !blockService.isBlocked(
                        userEmail,
                        friend.getFriend().getEmail()
                ))
                .map(friend -> {

                    boolean online =
                            onlineUserService.isOnline(friend.getFriend().getEmail());

                    String lastSeen =
                            formatLastSeen(friend.getFriend().getLastActiveAt());

                    return new FriendResponse(
                            friend.getFriend().getId(),
                            friend.getFriend().getEmail(),
                            friend.getFriend().getNickname(),
                            friend.getFriend().getProfileImageUrl(),
                            friend.getFriend().getStatusMessage(),
                            online,
                            lastSeen
                    );

                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getPendingRequests(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return friendRepository.findByFriendAndStatus(user, FriendStatus.PENDING)
                .stream()
                .filter(friend -> !blockService.isBlocked(
                        userEmail,
                        friend.getUser().getEmail()
                ))
                .map(friend -> {

                    boolean online =
                            onlineUserService.isOnline(friend.getUser().getEmail());

                    String lastSeen =
                            formatLastSeen(friend.getUser().getLastActiveAt());

                    return new FriendResponse(
                            friend.getUser().getId(),
                            friend.getUser().getEmail(),
                            friend.getUser().getNickname(),
                            friend.getUser().getProfileImageUrl(),
                            friend.getUser().getStatusMessage(),
                            online,
                            lastSeen
                    );

                })
                .toList();
    }

    private String formatLastSeen(LocalDateTime lastActiveAt){

        if(lastActiveAt == null){
            return "오래전 접속";
        }

        Duration duration =
                Duration.between(lastActiveAt, LocalDateTime.now());

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if(minutes < 60){
            return minutes + "분 전 접속";
        }

        if(hours < 24){
            return hours + "시간 전 접속";
        }

        return days + "일 전 접속";
    }

}