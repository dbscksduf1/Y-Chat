package com.yunchat.chat.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunchat.chat.domain.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        try {

            String msg = new String(message.getBody());

            ChatMessageResponse chatMessage =
                    objectMapper.readValue(msg, ChatMessageResponse.class);

            Long roomId = chatMessage.getRoomId();

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    chatMessage
            );

            log.debug("Redis message received for room {}", roomId);

        } catch (Exception e) {
            log.error("Redis message processing failed", e);
        }
    }
}