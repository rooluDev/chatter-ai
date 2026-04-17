package com.chatterai.dm.controller;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.attachment.repository.AttachmentRepository;
import com.chatterai.common.NotificationService;
import com.chatterai.config.RedisMessagePublisher;
import com.chatterai.dm.repository.DmUnreadCountRepository;
import com.chatterai.dm.service.DmService;
import com.chatterai.message.dto.ChatEventDto;
import com.chatterai.message.dto.ChatMessageResponseDto;
import com.chatterai.message.dto.ChatSendRequestDto;
import com.chatterai.message.entity.Message;
import com.chatterai.message.entity.MessageType;
import com.chatterai.message.repository.MessageRepository;
import com.chatterai.message.service.AiMessageService;
import com.chatterai.user.entity.User;
import com.chatterai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DmMessageController {

    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final DmService dmService;
    private final DmUnreadCountRepository dmUnreadCountRepository;
    private final NotificationService notificationService;
    private final RedisMessagePublisher redisPublisher;
    private final AiMessageService aiMessageService;

    /**
     * DM 메시지 전송.
     * /app/dm/{dmRoomId}/send
     */
    @MessageMapping("/dm/{dmRoomId}/send")
    public void sendDmMessage(@DestinationVariable Long dmRoomId,
                               @Payload ChatSendRequestDto request,
                               Principal principal) {
        Long userId = extractUserId(principal);

        // DM 방 참여자 검증
        if (!dmService.isParticipant(dmRoomId, userId)) {
            log.warn("Unauthorized DM send attempt: dmRoomId={}, userId={}", dmRoomId, userId);
            return;
        }

        // 메시지 저장
        MessageType type = parseMessageType(request.getType());
        Message message = Message.builder()
                .dmRoomId(dmRoomId)
                .userId(userId)
                .content(request.getContent())
                .type(type)
                .isAiMessage(false)
                .build();
        messageRepository.save(message);

        // 첨부파일 저장
        List<Attachment> attachments = saveAttachments(message.getId(), request);

        // 닉네임 조회
        String nickname = userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 상대방 안읽은 수 증가 + UNREAD_COUNT 알림 발송
        incrementDmUnreadAndNotify(dmRoomId, userId);

        // CHAT 이벤트 생성 (tempId echo → 클라이언트 optimistic 메시지 교체)
        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(message, nickname, attachments, Collections.emptyList());
        ChatEventDto event = ChatEventDto.builder()
                .eventType("CHAT")
                .tempId(request.getTempId())
                .message(msgDto)
                .build();
        redisPublisher.publish("chat:dm:" + dmRoomId, event);

        // @AI 멘션 감지 → DM AI 응답 처리
        if (request.getContent() != null && request.getContent().contains("@AI")) {
            String tempId = UUID.randomUUID().toString();
            aiMessageService.processDmAiMentionAsync(dmRoomId, userId, request.getContent(), tempId);
        }
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    /**
     * 발신자 제외 DM 참여자의 안읽음 카운트 증가 및 UNREAD_COUNT 알림 발송.
     */
    private void incrementDmUnreadAndNotify(Long dmRoomId, Long senderId) {
        dmService.getParticipantUserIds(dmRoomId).stream()
                .filter(uid -> !uid.equals(senderId))
                .forEach(uid -> {
                    dmUnreadCountRepository.increment(uid, dmRoomId);
                    long unreadCount = dmUnreadCountRepository.getCount(uid, dmRoomId);
                    notificationService.sendDmUnreadCount(uid, dmRoomId, unreadCount);
                });
    }

    private Long extractUserId(Principal principal) {
        if (principal == null) throw new IllegalStateException("No authenticated user");
        return Long.parseLong(principal.getName());
    }

    private MessageType parseMessageType(String type) {
        try {
            return MessageType.valueOf(type);
        } catch (Exception e) {
            return MessageType.TEXT;
        }
    }

    private List<Attachment> saveAttachments(Long messageId, ChatSendRequestDto request) {
        if (request.getAttachments().isEmpty()) return Collections.emptyList();

        List<Attachment> attachments = new ArrayList<>();
        List<ChatSendRequestDto.AttachmentRequest> reqs = request.getAttachments();

        for (int i = 0; i < reqs.size(); i++) {
            ChatSendRequestDto.AttachmentRequest req = reqs.get(i);
            try {
                Attachment attachment = Attachment.builder()
                        .messageId(messageId)
                        .fileUrl(req.getFileUrl())
                        .fileName(req.getFileName())
                        .fileSize(req.getFileSize())
                        .fileType(request.resolveFileType(req))
                        .sortOrder(i)
                        .build();
                attachments.add(attachmentRepository.save(attachment));
            } catch (Exception e) {
                // 파일 IO 오류는 메시지 트랜잭션에 영향 주지 않음 (CLAUDE.md 4.8)
                log.error("Failed to save attachment for messageId={}", messageId, e);
            }
        }
        return attachments;
    }
}
