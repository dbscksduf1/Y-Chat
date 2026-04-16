package com.yunchat.chat.domain.chat.service;

import com.yunchat.chat.domain.chat.entity.ChatMessage;
import com.yunchat.chat.domain.chat.entity.ChatRoom;
import com.yunchat.chat.domain.chat.entity.MessageRead;
import com.yunchat.chat.domain.chat.entity.RoomMember;
import com.yunchat.chat.domain.chat.entity.RoomType;
import com.yunchat.chat.domain.chat.repository.ChatMessageRepository;
import com.yunchat.chat.domain.chat.repository.ChatRoomRepository;
import com.yunchat.chat.domain.chat.repository.MessageReadRepository;
import com.yunchat.chat.domain.chat.repository.RoomMemberRepository;
import com.yunchat.chat.domain.user.repository.UserRepository;
import com.yunchat.chat.domain.user.service.BlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageServiceTest {

    @Mock RoomMemberRepository roomMemberRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock MessageReadRepository messageReadRepository;
    @Mock BlockService blockService;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks ChatMessageService chatMessageService;

    private ChatRoom room1;
    private ChatRoom room2;

    @BeforeEach
    void setUp() {
        room1 = createRoom(1L, "room-one", RoomType.GROUP);
        room2 = createRoom(2L, "room-two", RoomType.PRIVATE);
    }

    // =========================================================
    // getMyChatRooms — N+1 검증
    // =========================================================

    @Test
    @DisplayName("getMyChatRooms: findByRoomIds 와 findLastMessagesPerRoom 을 각 1회씩 호출한다")
    void getMyChatRooms_usesBatchQueries() {
        String me = "me@test.com";

        List<RoomMember> memberships = List.of(
                new RoomMember(room1, me),
                new RoomMember(room2, me)
        );

        given(roomMemberRepository.findByUsername(me)).willReturn(memberships);
        given(chatMessageRepository.countUnreadGroupedByRoom(me)).willReturn(List.of());
        given(roomMemberRepository.findByRoomIds(List.of(1L, 2L))).willReturn(memberships);
        given(chatMessageRepository.findLastMessagesPerRoom(List.of(1L, 2L))).willReturn(List.of());
        given(blockService.isBlocked(anyString(), anyString())).willReturn(false);

        chatMessageService.getMyChatRooms(me);

        // 배치 쿼리 각 1회
        verify(roomMemberRepository, times(1)).findByRoomIds(List.of(1L, 2L));
        verify(chatMessageRepository, times(1)).findLastMessagesPerRoom(List.of(1L, 2L));

        // 루프 안에서 방별로 개별 조회하지 않음
        verify(roomMemberRepository, never()).findByRoomId(anyLong());
        verify(chatMessageRepository, never()).findByRoomAndDeletedFalse(any(), any());
    }

    @Test
    @DisplayName("getMyChatRooms: RANDOM 타입 채팅방은 결과에서 제외된다")
    void getMyChatRooms_excludesRandomRooms() {
        String me = "me@test.com";
        ChatRoom randomRoom = createRoom(99L, "random-room", RoomType.RANDOM);

        List<RoomMember> memberships = List.of(
                new RoomMember(room1, me),
                new RoomMember(randomRoom, me)
        );

        given(roomMemberRepository.findByUsername(me)).willReturn(memberships);
        given(chatMessageRepository.countUnreadGroupedByRoom(me)).willReturn(List.of());
        given(roomMemberRepository.findByRoomIds(List.of(1L))).willReturn(List.of(new RoomMember(room1, me)));
        given(chatMessageRepository.findLastMessagesPerRoom(List.of(1L))).willReturn(List.of());
        given(blockService.isBlocked(anyString(), anyString())).willReturn(false);

        var result = chatMessageService.getMyChatRooms(me);

        // room1 만 포함, randomRoom 제외
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getMyChatRooms: 멤버십이 없으면 빈 리스트를 반환한다")
    void getMyChatRooms_returnsEmptyWhenNoMembership() {
        given(roomMemberRepository.findByUsername("nobody@test.com")).willReturn(List.of());

        var result = chatMessageService.getMyChatRooms("nobody@test.com");

        assertThat(result).isEmpty();
        verifyNoInteractions(chatMessageRepository);
    }

    // =========================================================
    // markAsRead — 배치 저장 검증
    // =========================================================

    @Test
    @DisplayName("markAsRead: unread 메시지를 saveAll 단일 배치로 저장한다")
    void markAsRead_usesSaveAll() {
        String email = "reader@test.com";
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room1));

        ChatMessage msg1 = createMessage(room1, "other@test.com", "hello");
        ChatMessage msg2 = createMessage(room1, "other@test.com", "world");
        given(chatMessageRepository.findUnreadMessages(1L, email)).willReturn(List.of(msg1, msg2));

        chatMessageService.markAsRead(1L, email);

        // saveAll 1회 (배치)
        verify(messageReadRepository, times(1)).saveAll(anyList());
        // 개별 save 미호출
        verify(messageReadRepository, never()).save(any(MessageRead.class));
    }

    @Test
    @DisplayName("markAsRead: 읽지 않은 메시지가 없으면 saveAll 을 호출하지 않는다")
    void markAsRead_skipsWhenNothingUnread() {
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room1));
        given(chatMessageRepository.findUnreadMessages(1L, "reader@test.com")).willReturn(List.of());

        chatMessageService.markAsRead(1L, "reader@test.com");

        verify(messageReadRepository, never()).saveAll(any());
        verify(messageReadRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead: saveAll 에 전달되는 MessageRead 목록의 크기가 unread 메시지 수와 같다")
    void markAsRead_savesExactCount() {
        String email = "reader@test.com";
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room1));

        List<ChatMessage> unread = List.of(
                createMessage(room1, "a@test.com", "1"),
                createMessage(room1, "a@test.com", "2"),
                createMessage(room1, "a@test.com", "3")
        );
        given(chatMessageRepository.findUnreadMessages(1L, email)).willReturn(unread);

        chatMessageService.markAsRead(1L, email);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageRead>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageReadRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    // =========================================================
    // markAsReadAndReturnMessages — 배치 저장 검증
    // =========================================================

    @Test
    @DisplayName("markAsReadAndReturnMessages: saveAll 단일 배치 호출 후 unread 목록을 반환한다")
    void markAsReadAndReturnMessages_usesSaveAll() {
        String email = "user@test.com";
        ChatMessage msg = createMessage(room1, "other@test.com", "hi");
        given(chatMessageRepository.findUnreadMessages(1L, email)).willReturn(List.of(msg));

        var result = chatMessageService.markAsReadAndReturnMessages(1L, email);

        verify(messageReadRepository, times(1)).saveAll(anyList());
        verify(messageReadRepository, never()).save(any(MessageRead.class));
        assertThat(result).hasSize(1);
    }

    // =========================================================
    // helpers
    // =========================================================

    private static ChatRoom createRoom(Long id, String name, RoomType type) {
        ChatRoom room = new ChatRoom(name, type);
        // 리플렉션으로 id 주입 (테스트 전용)
        try {
            var field = ChatRoom.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(room, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return room;
    }

    private static ChatMessage createMessage(ChatRoom room, String sender, String content) {
        return ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .deleted(false)
                .build();
    }
}
