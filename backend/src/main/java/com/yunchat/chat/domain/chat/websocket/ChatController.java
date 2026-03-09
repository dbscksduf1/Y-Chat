package com.yunchat.chat.domain.chat.websocket;

import com.yunchat.chat.domain.chat.dto.*;
import com.yunchat.chat.domain.chat.entity.ChatMessage;
import com.yunchat.chat.domain.chat.service.ChatMessageService;
import com.yunchat.chat.domain.user.service.BlockService;
import com.yunchat.chat.global.redis.RedisPublisher;
import com.yunchat.chat.global.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.yunchat.chat.domain.random.service.RandomChatService;
import com.yunchat.chat.domain.random.service.RandomRoom;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class ChatController {


    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineUserService onlineUserService;
    private final RedisPublisher redisPublisher;
    private final BlockService blockService;
    private final RandomChatService randomChatService;


    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {


        String senderEmail = principal.getName();
        Long roomId = request.getRoomId();

        // ⭐ 랜덤채팅 방 체크
        RandomRoom randomRoom = randomChatService.getRoom(roomId);

        if (randomRoom != null || roomId > 1000000000000L) {

            ChatMessageResponse message = new ChatMessageResponse(
                    null,
                    senderEmail,
                    request.getContent(),
                    LocalDateTime.now(),
                    null,
                    0
            );

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    message
            );

            return;
        }

        // 기존 일반채팅 로직 그대로 유지

        ChatMessageResponse savedMessage =
                chatMessageService.saveMessage(request, senderEmail);

        // Redis publish
        redisPublisher.publish(savedMessage);

        // WebSocket broadcast
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                savedMessage
        );

        List<String> memberEmails =
                chatMessageService.getRoomMemberEmails(roomId);

        for (String email : memberEmails) {

            if (!email.equals(senderEmail)) {

                if (blockService.isBlocked(senderEmail, email)) {
                    continue;
                }

                if (onlineUserService.isUserInRoom(email, roomId)) {

                    chatMessageService.markSingleMessageAsRead(
                            savedMessage.getId(),
                            email
                    );

                    messagingTemplate.convertAndSendToUser(
                            senderEmail,
                            "/queue/read",
                            new ReadResponse(
                                    savedMessage.getId(),
                                    roomId
                            )
                    );

                } else {

                    messagingTemplate.convertAndSendToUser(
                            email,
                            "/queue/unread",
                            new UnreadUpdateResponse(roomId)
                    );

                }
            }
        }
    }


    @MessageMapping("/chat.enter")
    public void enterRoom(ChatRoomEnterRequest request, Principal principal) {

        String email = principal.getName();
        Long roomId = request.getRoomId();

        onlineUserService.enterRoom(email, roomId);

        List<ChatMessage> readMessages =
                chatMessageService.markAsReadAndReturnMessages(roomId, email);

        for (ChatMessage msg : readMessages) {

            messagingTemplate.convertAndSendToUser(
                    msg.getSender(),
                    "/queue/read",
                    new ReadResponse(msg.getId(), roomId)
            );
        }
    }
    @MessageMapping("/chat.leave")
    public void leaveRoom(ChatRoomEnterRequest request, Principal principal) {

        String email = principal.getName();
        Long roomId = request.getRoomId();


        onlineUserService.leaveRoom(email, roomId);

    }
    @MessageMapping("/chat.random.leave")
    public void leaveRandomRoom(ChatRoomEnterRequest request, Principal principal) {

        Long roomId = request.getRoomId();

        ChatMessageResponse message = new ChatMessageResponse(
                null,
                "system",
                "상대방이 나갔습니다",
                java.time.LocalDateTime.now(),
                null,
                0
        );

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                message
        );

        randomChatService.leave(roomId);
        randomChatService.cancel(principal.getName());
    }
}