package com.yunchat.chat.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OnlineUserService {


    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> activeRoomMap = new ConcurrentHashMap<>();

    public void addUser(String username) {
        onlineUsers.add(username);
    }

    public void removeUser(String username) {
        onlineUsers.remove(username);
        activeRoomMap.remove(username);
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    public void enterRoom(String email, Long roomId) {

        activeRoomMap.put(email, roomId);

        log.debug("ENTER ROOM: {} -> {}", email, roomId);

    }

    public void leaveRoom(String email, Long roomId) {

        Long current = activeRoomMap.get(email);

        if (current != null && current.equals(roomId)) {
            activeRoomMap.remove(email);
            log.debug("LEAVE ROOM: {} -> {}", email, roomId);
        }

    }

    public boolean isUserInRoom(String email, Long roomId) {

        Long current = activeRoomMap.get(email);

        log.debug("isUserInRoom CHECK: {} 현재저장값={} 비교방={}", email, current, roomId);

        return current != null && current.equals(roomId);

    }


}
