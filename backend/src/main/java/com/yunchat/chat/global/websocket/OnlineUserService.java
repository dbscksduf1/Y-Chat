package com.yunchat.chat.global.websocket;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

        System.out.println("ENTER ROOM: " + email + " -> " + roomId);

    }

    public void leaveRoom(String email, Long roomId) {

        Long current = activeRoomMap.get(email);

        if (current != null && current.equals(roomId)) {
            activeRoomMap.remove(email);
            System.out.println("LEAVE ROOM: " + email + " -> " + roomId);
        }

    }

    public boolean isUserInRoom(String email, Long roomId) {

        Long current = activeRoomMap.get(email);

        System.out.println("isUserInRoom CHECK: "
                + email + " 현재저장값=" + current + " 비교방=" + roomId);

        return current != null && current.equals(roomId);

    }


}
