package com.yunchat.chat.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yunchat.chat.domain.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void onMessage(Message message, byte[] pattern) {

        try {

            String msg = new String(message.getBody());

            ChatMessageResponse chatMessage =
                    objectMapper.readValue(msg, ChatMessageResponse.class);

            Long roomId = chatMessage.getId(); // 네 코드 기준

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    chatMessage
            );

            System.out.println("Redis Message: " + msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}