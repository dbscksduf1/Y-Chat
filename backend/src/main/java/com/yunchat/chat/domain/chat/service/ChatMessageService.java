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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

        List<RoomMember> memberships =
                roomMemberRepository.findByUsername(username);

        List<Object[]> unreadResults =
                chatMessageRepository.countUnreadGroupedByRoom(username);

        Map<Long, Long> unreadMap = new HashMap<>();
        for (Object[] row : unreadResults) {
            unreadMap.put((Long) row[0], (Long) row[1]);
        }

        List<ChatRoomListResponse> result = new ArrayList<>();

        for (RoomMember member : memberships) {

            ChatRoom room = member.getRoom();

            // 🔥 RANDOM 채팅방은 목록에서 제외
            if (room.getRoomType() == RoomType.RANDOM) {
                continue;
            }

            List<String> members = roomMemberRepository.findByRoomId(room.getId())
                    .stream()
                    .map(RoomMember::getUsername)
                    .toList();

            for (String other : members) {
                if (!other.equals(username) && blockService.isBlocked(username, other)) {
                    room = null;
                    break;
                }
            }

            if (room == null) continue;

            List<ChatMessage> messages =
                    chatMessageRepository.findByRoomAndDeletedFalse(
                            room,
                            org.springframework.data.domain.PageRequest.of(
                                    0,
                                    1,
                                    org.springframework.data.domain.Sort.by("createdAt").descending()
                            )
                    ).getContent();

            ChatMessage lastMessage =
                    messages.isEmpty() ? null : messages.get(0);

            Long unreadCount =
                    unreadMap.getOrDefault(room.getId(), 0L);

            result.add(
                    new ChatRoomListResponse(
                            room.getId(),
                            room.getRoomName(),
                            lastMessage != null ? lastMessage.getContent() : null,
                            lastMessage != null ? lastMessage.getCreatedAt() : null,
                            unreadCount
                    )
            );
        }

        result.sort(
                Comparator.comparing(
                        ChatRoomListResponse::getLastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        return result;
    }

    public void markAsRead(Long roomId, String username) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<ChatMessage> messages =
                chatMessageRepository.findByRoomAndDeletedFalse(
                        room,
                        org.springframework.data.domain.Pageable.unpaged()
                ).getContent();

        for (ChatMessage message : messages) {

            boolean exists = messageReadRepository
                    .existsByMessageAndUsername(message, username);

            if (!exists) {
                messageReadRepository.save(
                        new MessageRead(message, username)
                );
            }
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

    public List<ChatMessageResponse> getMessages(Long roomId) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<ChatMessage> messages =
                chatMessageRepository.findByRoomAndDeletedFalse(
                        room,
                        org.springframework.data.domain.Pageable.unpaged()
                ).getContent();

        return messages.stream()
                .map(message -> {

                    User senderUser = userRepository
                            .findByEmail(message.getSender())
                            .orElse(null);

                    String profileImageUrl =
                            senderUser != null ? senderUser.getProfileImageUrl() : null;

                    int unreadCount = 0;

                    return new ChatMessageResponse(
                            message.getId(),
                            message.getSender(),
                            message.getContent(),
                            message.getCreatedAt(),
                            profileImageUrl,
                            unreadCount
                    );
                })
                .toList();
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

        List<ChatMessage> unreadMessages =
                chatMessageRepository.findUnreadMessages(roomId, email);

        for (ChatMessage msg : unreadMessages) {

            boolean exists = messageReadRepository
                    .existsByMessageAndUsername(msg, email);

            if (!exists) {
                messageReadRepository.save(
                        new MessageRead(msg, email)
                );
            }
        }

        return unreadMessages;
    }
}