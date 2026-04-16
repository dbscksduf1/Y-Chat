package com.yunchat.chat.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    public void publish(Object message) {

        try {

            String json = objectMapper.writeValueAsString(message);

            redisTemplate.convertAndSend("chat", json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}