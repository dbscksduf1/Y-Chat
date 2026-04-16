package com.yunchat.chat.global.websocket;

import com.yunchat.chat.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineUserService onlineUserService;

    // 🔥 추가
    private final UserService userService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        Principal user = accessor.getUser();

        if (user != null) {
            onlineUserService.addUser(user.getName());
            log.debug("WebSocket Connected: {}", user.getName());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        Principal user = accessor.getUser();

        if (user != null) {

            String email = user.getName();

            onlineUserService.removeUser(email);

            // 🔥 추가
            userService.updateLastActive(email);

            log.debug("WebSocket Disconnected: {}", email);
        }
    }
}