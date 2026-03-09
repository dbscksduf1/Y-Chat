package com.yunchat.chat.domain.random.service;

import com.yunchat.chat.domain.random.dto.RandomMatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RandomChatService {

    private final StringRedisTemplate stringRedisTemplate;

    private final Map<Long, RandomRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> userRoom = new ConcurrentHashMap<>();

    private static final String QUEUE_KEY = "random:queue";

    public RandomMatchResponse enter(String email) {

        stringRedisTemplate.opsForList().remove(QUEUE_KEY, 1, email);

        if (userRoom.containsKey(email)) {

            Long roomId = userRoom.get(email);

            if (rooms.containsKey(roomId)) {
                return new RandomMatchResponse("MATCHED", roomId);
            }

            userRoom.remove(email);
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

        RandomRoom room = new RandomRoom(roomId, opponent, email, true);

        userRoom.put(email, roomId);
        userRoom.put(opponent, roomId);

        rooms.put(roomId, room);

        return new RandomMatchResponse("MATCHED", roomId);
    }

    public void cancel(String email) {

        stringRedisTemplate.opsForList().remove(QUEUE_KEY, 1, email);
        userRoom.remove(email);
    }

    public void leave(Long roomId) {

        RandomRoom room = rooms.remove(roomId);

        if (room != null) {

            userRoom.remove(room.getUserA());
            userRoom.remove(room.getUserB());
        }
    }

    public RandomRoom getRoom(Long roomId) {

        return rooms.get(roomId);
    }
}