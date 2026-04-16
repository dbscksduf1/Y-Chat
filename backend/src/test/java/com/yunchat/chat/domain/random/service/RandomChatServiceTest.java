package com.yunchat.chat.domain.random.service;

import com.yunchat.chat.domain.random.dto.RandomMatchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RandomChatServiceTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ListOperations<String, String> listOps;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks RandomChatService randomChatService;

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForList()).willReturn(listOps);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
    }

    // =========================================================
    // enter — 대기(WAIT)
    // =========================================================

    @Test
    @DisplayName("enter: 큐가 비어 있으면 큐에 등록하고 WAIT 을 반환한다")
    void enter_returnsWait_whenQueueEmpty() {
        given(valueOps.get("random:user:me@test.com")).willReturn(null);
        given(listOps.size("random:queue")).willReturn(0L);

        RandomMatchResponse result = randomChatService.enter("me@test.com");

        assertThat(result.getStatus()).isEqualTo("WAIT");
        assertThat(result.getRoomId()).isNull();
        verify(listOps).leftPush("random:queue", "me@test.com");
    }

    @Test
    @DisplayName("enter: 큐에 자기 자신만 있으면 WAIT 을 반환한다")
    void enter_returnsWait_whenOnlySelfInQueue() {
        given(valueOps.get("random:user:me@test.com")).willReturn(null);
        given(listOps.size("random:queue")).willReturn(1L);
        // 꺼냈더니 자기 자신
        given(listOps.rightPop("random:queue")).willReturn("me@test.com");

        RandomMatchResponse result = randomChatService.enter("me@test.com");

        assertThat(result.getStatus()).isEqualTo("WAIT");
        // 다시 큐에 넣었는지 확인
        verify(listOps).leftPush("random:queue", "me@test.com");
    }

    // =========================================================
    // enter — 매칭(MATCHED)
    // =========================================================

    @Test
    @DisplayName("enter: 큐에 다른 사람이 있으면 방을 생성하고 MATCHED 를 반환한다")
    void enter_returnsMatched_whenOpponentInQueue() {
        given(valueOps.get("random:user:me@test.com")).willReturn(null);
        given(listOps.size("random:queue")).willReturn(1L);
        given(listOps.rightPop("random:queue")).willReturn("opponent@test.com");

        RandomMatchResponse result = randomChatService.enter("me@test.com");

        assertThat(result.getStatus()).isEqualTo("MATCHED");
        assertThat(result.getRoomId()).isNotNull();

        // 두 유저 모두 roomId 가 저장됐는지 확인
        verify(valueOps).set(eq("random:user:me@test.com"), anyString(), any());
        verify(valueOps).set(eq("random:user:opponent@test.com"), anyString(), any());
        // 방 정보도 저장됐는지 확인
        verify(valueOps).set(contains("random:room:"), anyString(), any());
    }

    @Test
    @DisplayName("enter: 이미 매칭된 방이 존재하면 재사용하고 MATCHED 를 반환한다")
    void enter_returnsMatched_whenAlreadyMatched() {
        Long existingRoomId = 12345L;
        given(valueOps.get("random:user:me@test.com")).willReturn(existingRoomId.toString());
        // 방 정보가 Redis 에 존재
        given(valueOps.get("random:room:" + existingRoomId))
                .willReturn("opponent@test.com|me@test.com");

        RandomMatchResponse result = randomChatService.enter("me@test.com");

        assertThat(result.getStatus()).isEqualTo("MATCHED");
        assertThat(result.getRoomId()).isEqualTo(existingRoomId);
        // 새 방을 만들지 않음 — listOps.rightPop 미호출
        verify(listOps, never()).rightPop(anyString());
    }

    @Test
    @DisplayName("enter: 저장된 방 정보가 만료되었으면 새로 매칭을 시도한다")
    void enter_retrysMatch_whenRoomExpired() {
        Long staleRoomId = 99999L;
        given(valueOps.get("random:user:me@test.com")).willReturn(staleRoomId.toString());
        // 방 키가 만료됨 → null
        given(valueOps.get("random:room:" + staleRoomId)).willReturn(null);
        // 재매칭 시도 — 큐가 비어 있음
        given(listOps.size("random:queue")).willReturn(0L);

        RandomMatchResponse result = randomChatService.enter("me@test.com");

        // 만료된 user 키 삭제됐는지 확인
        verify(stringRedisTemplate).delete("random:user:me@test.com");
        assertThat(result.getStatus()).isEqualTo("WAIT");
    }

    // =========================================================
    // cancel
    // =========================================================

    @Test
    @DisplayName("cancel: 큐에서 이메일을 제거하고 user 키를 삭제한다")
    void cancel_removesFromQueueAndDeletesUserKey() {
        randomChatService.cancel("me@test.com");

        verify(listOps).remove("random:queue", 1, "me@test.com");
        verify(stringRedisTemplate).delete("random:user:me@test.com");
    }

    // =========================================================
    // leave
    // =========================================================

    @Test
    @DisplayName("leave: 방 키와 두 유저 키를 모두 삭제한다")
    void leave_deletesRoomAndBothUserKeys() {
        Long roomId = 777L;
        given(valueOps.get("random:room:" + roomId))
                .willReturn("userA@test.com|userB@test.com");

        randomChatService.leave(roomId);

        verify(stringRedisTemplate).delete("random:room:" + roomId);
        verify(stringRedisTemplate).delete("random:user:userA@test.com");
        verify(stringRedisTemplate).delete("random:user:userB@test.com");
    }

    @Test
    @DisplayName("leave: 방이 존재하지 않으면 아무것도 삭제하지 않는다")
    void leave_doesNothing_whenRoomNotFound() {
        given(valueOps.get(startsWith("random:room:"))).willReturn(null);

        randomChatService.leave(999L);

        verify(stringRedisTemplate, never()).delete(anyString());
    }

    // =========================================================
    // getRoom
    // =========================================================

    @Test
    @DisplayName("getRoom: Redis 에 방 데이터가 있으면 RandomRoom 을 반환한다")
    void getRoom_returnsRoom_whenExists() {
        given(valueOps.get("random:room:1")).willReturn("a@test.com|b@test.com");

        RandomRoom room = randomChatService.getRoom(1L);

        assertThat(room).isNotNull();
        assertThat(room.getUserA()).isEqualTo("a@test.com");
        assertThat(room.getUserB()).isEqualTo("b@test.com");
    }

    @Test
    @DisplayName("getRoom: Redis 에 방 데이터가 없으면 null 을 반환한다")
    void getRoom_returnsNull_whenNotExists() {
        given(valueOps.get("random:room:1")).willReturn(null);

        assertThat(randomChatService.getRoom(1L)).isNull();
    }
}
