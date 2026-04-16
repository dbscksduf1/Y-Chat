package com.yunchat.chat.global.websocket;

import com.yunchat.chat.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketUserInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String bearer = accessor.getFirstNativeHeader("Authorization");

            if (bearer != null && bearer.startsWith("Bearer ")) {
                String token = bearer.substring(7);
                if (jwtProvider.validateToken(token)) {
                    String email = jwtProvider.getSubject(token);
                    accessor.setUser(() -> email);
                }
            }
        }

        return message;
    }
}