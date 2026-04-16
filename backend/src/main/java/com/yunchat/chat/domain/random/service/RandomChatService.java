package com.yunchat.chat.domain.random.service;

import com.yunchat.chat.domain.random.dto.RandomMatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RandomChatService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String QUEUE_KEY    = "random:queue";
    private static final String ROOM_PREFIX  = "random:room:";
    private static final String USER_PREFIX  = "random:user:";
    private static final Duration ROOM_TTL   = Duration.ofHours(24);

    public RandomMatchResponse enter(String email) {

        stringRedisTemplate.opsForList().remove(QUEUE_KEY, 1, email);

        // 이미 매칭된 방이 있으면 반환
        String existingRoomId = stringRedisTemplate.opsForValue().get(USER_PREFIX + email);
        if (existingRoomId != null) {
            Long roomId = Long.parseLong(existingRoomId);
            if (getRoom(roomId) != null) {
                return new RandomMatchResponse("MATCHED", roomId);
            }
            stringRedisTemplate.delete(USER_PREFIX + email);
        }

        Long size = stringRedisTemplate.opsForList().size(QUEUE_KEY);

        if (size == null || size == 0) {
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, email);
            return new RandomMatchResponse("WAIT", null);
        }

        String opponent = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY);

        if (opponent == null || opponent.equals(email)) {
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, email);
            return new RandomMatchResponse("WAIT", null);
        }

        Long roomId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        // 방 정보를 Redis에 저장 (userA|userB 형식)
        stringRedisTemplate.opsForValue().set(
                ROOM_PREFIX + roomId,
                opponent + "|" + email,
                ROOM_TTL
        );

        stringRedisTemplate.opsForValue().set(USER_PREFIX + email, roomId.toString(), ROOM_TTL);
        stringRedisTemplate.opsForValue().set(USER_PREFIX + opponent, roomId.toString(), ROOM_TTL);

        return new RandomMatchResponse("MATCHED", roomId);
    }

    public void cancel(String email) {
        stringRedisTemplate.opsForList().remove(QUEUE_KEY, 1, email);
        stringRedisTemplate.delete(USER_PREFIX + email);
    }

    public void leave(Long roomId) {
        RandomRoom room = getRoom(roomId);
        if (room != null) {
            stringRedisTemplate.delete(ROOM_PREFIX + roomId);
            stringRedisTemplate.delete(USER_PREFIX + room.getUserA());
            stringRedisTemplate.delete(USER_PREFIX + room.getUserB());
        }
    }

    public RandomRoom getRoom(Long roomId) {
        String value = stringRedisTemplate.opsForValue().get(ROOM_PREFIX + roomId);
        if (value == null) return null;
        String[] parts = value.split("\\|", 2);
        return new RandomRoom(roomId, parts[0], parts[1], true);
    }
}