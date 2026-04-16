package com.yunchat.chat.domain.chat.service;

import com.yunchat.chat.domain.chat.dto.ChatMessageRequest;
import com.yunchat.chat.domain.chat.dto.ChatMessageResponse;
import com.yunchat.chat.domain.chat.dto.ChatRoomListResponse;
import com.yunchat.chat.domain.chat.entity.*;
import com.yunchat.chat.domain.chat.repository.ChatMessageRepository;
import com.yunchat.chat.domain.chat.repository.ChatRoomRepository;
import com.yunchat.chat.domain.chat.repository.MessageReadRepository;
import com.yunchat.chat.domain.chat.repository.RoomMemberRepository;
import com.yunchat.chat.domain.user.entity.User;
import com.yunchat.chat.domain.user.repository.UserRepository;
import com.yunchat.chat.domain.user.service.BlockService;
import com.yunchat.chat.global.exception.CustomException;
import com.yunchat.chat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final RoomMemberRepository roomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageReadRepository messageReadRepository;
    private final BlockService blockService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<ChatRoomListResponse> getMyChatRooms(String username) {

        List<RoomMember> memberships = roomMemberRepository.findByUsername(username);

        // RANDOM 채팅방 제외, roomId 목록 수집
        List<Long> roomIds = memberships.stream()
                .filter(m -> m.getRoom().getRoomType() != RoomType.RANDOM)
                .map(m -> m.getRoom().getId())
                .distinct()
                .toList();

        if (roomIds.isEmpty()) return Collections.emptyList();

        // 읽지 않은 메시지 수 (1 쿼리)
        Map<Long, Long> unreadMap = new HashMap<>();
        for (Object[] row : chatMessageRepository.countUnreadGroupedByRoom(username)) {
            unreadMap.put((Long) row[0], (Long) row[1]);
        }

        // 각 방의 전체 멤버 (1 쿼리)
        Map<Long, List<String>> membersMap = roomMemberRepository.findByRoomIds(roomIds)
                .stream()
                .collect(Collectors.groupingBy(
                        rm -> rm.getRoom().getId(),
                        Collectors.mapping(RoomMember::getUsername, Collectors.toList())
                ));

        // 각 방의 마지막 메시지 (1 쿼리)
        Map<Long, ChatMessage> lastMessageMap = chatMessageRepository.findLastMessagesPerRoom(roomIds)
                .stream()
                .collect(Collectors.toMap(m -> m.getRoom().getId(), m -> m));

        List<ChatRoomListResponse> result = new ArrayList<>();

        for (RoomMember membership : memberships) {

            ChatRoom room = membership.getRoom();

            if (room.getRoomType() == RoomType.RANDOM) continue;

            List<String> members = membersMap.getOrDefault(room.getId(), Collections.emptyList());

            boolean blocked = members.stream()
                    .filter(other -> !other.equals(username))
                    .anyMatch(other -> blockService.isBlocked(username, other));

            if (blocked) continue;

            ChatMessage lastMessage = lastMessageMap.get(room.getId());
            Long unreadCount = unreadMap.getOrDefault(room.getId(), 0L);

            result.add(new ChatRoomListResponse(
                    room.getId(),
                    room.getRoomName(),
                    lastMessage != null ? lastMessage.getContent() : null,
                    lastMessage != null ? lastMessage.getCreatedAt() : null,
                    unreadCount
            ));
        }

        result.sort(Comparator.comparing(
                ChatRoomListResponse::getLastMessageTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return result;
    }

    public void markAsRead(Long roomId, String username) {

        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<ChatMessage> unread = chatMessageRepository.findUnreadMessages(roomId, username);

        if (!unread.isEmpty()) {
            messageReadRepository.saveAll(
                    unread.stream().map(m -> new MessageRead(m, username)).toList()
            );
        }
    }

    public void deleteMessage(Long messageId, String username) {

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getSender().equals(username)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MEMBER);
        }

        message.delete();
        chatMessageRepository.save(message);
    }

    public void leaveRoom(Long roomId, String username) {

        RoomMember member = roomMemberRepository
                .findByRoomIdAndUsername(roomId, username)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        roomMemberRepository.delete(member);
    }

    public Long createPrivateRoom(String creatorEmail, String targetEmail) {

        if (creatorEmail.equals(targetEmail)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (blockService.isBlocked(creatorEmail, targetEmail)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String roomName;

        if (creatorEmail.compareTo(targetEmail) < 0) {
            roomName = creatorEmail + "_" + targetEmail;
        } else {
            roomName = targetEmail + "_" + creatorEmail;
        }

        Optional<ChatRoom> existing =
                chatRoomRepository.findByRoomName(roomName);

        if (existing.isPresent()) {

            ChatRoom room = existing.get();

            if (!roomMemberRepository.existsByRoomIdAndUsername(room.getId(), creatorEmail)) {
                roomMemberRepository.save(new RoomMember(room, creatorEmail));
            }

            if (!roomMemberRepository.existsByRoomIdAndUsername(room.getId(), targetEmail)) {
                roomMemberRepository.save(new RoomMember(room, targetEmail));
            }

            return room.getId();
        }

        ChatRoom room = new ChatRoom(roomName, RoomType.PRIVATE);
        chatRoomRepository.save(room);

        roomMemberRepository.save(new RoomMember(room, creatorEmail));
        roomMemberRepository.save(new RoomMember(room, targetEmail));

        return room.getId();
    }

    public void inviteMember(Long roomId, String inviter, String newMemberUsername) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!roomMemberRepository.existsByRoomIdAndUsername(roomId, inviter)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MEMBER);
        }

        if (blockService.isBlocked(inviter, newMemberUsername)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (roomMemberRepository.existsByRoomIdAndUsername(roomId, newMemberUsername)) {
            throw new CustomException(ErrorCode.USER_ALREADY_IN_ROOM);
        }

        roomMemberRepository.save(
                new RoomMember(room, newMemberUsername)
        );

        List<String> members = roomMemberRepository.findByRoomId(roomId)
                .stream()
                .map(RoomMember::getUsername)
                .toList();

        String newRoomName = String.join(" ", members) + " (" + members.size() + ")";

        room.setRoomName(newRoomName);

        chatRoomRepository.save(room);
    }

    public List<ChatMessageResponse> getMessages(Long roomId, Long cursorId) {

        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        int size = 20;

        Pageable pageable = PageRequest.of(0, size);

        List<ChatMessageResponse> messages =
                chatMessageRepository.findMessagesWithCursor(roomId, cursorId, pageable);

        // 최신순으로 가져왔으면 뒤집어서 반환 (채팅 UI 맞추기)
        Collections.reverse(messages);

        return messages;
    }

    public List<String> getRoomMemberEmails(Long roomId) {
        return roomMemberRepository.findByRoomId(roomId)
                .stream()
                .map(RoomMember::getUsername)
                .toList();
    }

    public ChatMessageResponse saveMessage(ChatMessageRequest request, String senderEmail) {

        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // 🔥 RANDOM 채팅은 DB 저장 안함
        if (room.getRoomType() == RoomType.RANDOM) {
            return null;
        }

        List<String> members = roomMemberRepository.findByRoomId(room.getId())
                .stream()
                .map(RoomMember::getUsername)
                .toList();

        for (String member : members) {
            if (!member.equals(senderEmail)
                    && blockService.isBlocked(senderEmail, member)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
        }

        ChatMessage message = new ChatMessage(
                room,
                senderEmail,
                request.getContent(),
                false
        );

        chatMessageRepository.save(message);

        User senderUser = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int unreadCount = (int) members.stream()
                .filter(member -> !member.equals(senderEmail))
                .count();

        for (String member : members) {
            if (!member.equals(senderEmail)) {
                messagingTemplate.convertAndSendToUser(
                        member,
                        "/queue/chatroom-update",
                        "update"
                );
            }
        }

        return new ChatMessageResponse(
                message.getId(),
                room.getId(),
                message.getSender(),
                message.getContent(),
                message.getCreatedAt(),
                senderUser.getProfileImageUrl(),
                unreadCount
        );
    }

    public void markSingleMessageAsRead(Long messageId, String username) {

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        boolean exists = messageReadRepository
                .existsByMessageAndUsername(message, username);

        if (!exists) {
            messageReadRepository.save(
                    new MessageRead(message, username)
            );
        }
    }

    public List<ChatMessage> markAsReadAndReturnMessages(Long roomId, String email) {

        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(roomId, email);

        if (!unreadMessages.isEmpty()) {
            messageReadRepository.saveAll(
                    unreadMessages.stream().map(m -> new MessageRead(m, email)).toList()
            );
        }

        return unreadMessages;
    }
}