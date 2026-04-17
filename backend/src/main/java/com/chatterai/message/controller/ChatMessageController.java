package com.chatterai.message.controller;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.attachment.repository.AttachmentRepository;
import com.chatterai.channel.entity.ChannelParticipant;
import com.chatterai.channel.repository.ChannelParticipantRepository;
import com.chatterai.channel.repository.UnreadCountRepository;
import com.chatterai.common.NotificationService;
import com.chatterai.config.RedisMessagePublisher;
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
public class ChatMessageController {

    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final UnreadCountRepository unreadCountRepository;
    private final NotificationService notificationService;
    private final RedisMessagePublisher redisPublisher;
    private final AiMessageService aiMessageService;

    /**
     * 채널 메시지 전송.
     * /app/channel/{channelId}/send
     */
    @MessageMapping("/channel/{channelId}/send")
    public void sendChannelMessage(@DestinationVariable Long channelId,
                                   @Payload ChatSendRequestDto request,
                                   Principal principal) {
        Long userId = extractUserId(principal);

        // 메시지 저장
        MessageType type = parseMessageType(request.getType());
        Message message = Message.builder()
                .channelId(channelId)
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

        // CHAT 이벤트 생성 (tempId echo → 클라이언트 optimistic 메시지 교체)
        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(message, nickname, attachments, Collections.emptyList());
        ChatEventDto event = ChatEventDto.builder()
                .eventType("CHAT")
                .tempId(request.getTempId())
                .message(msgDto)
                .build();

        // Redis Pub/Sub 발행 → RedisMessageSubscriber가 STOMP 브로드캐스트
        redisPublisher.publish("chat:channel:" + channelId, event);

        // 발신자 제외 참여자 안읽음 카운트 증가 + UNREAD_COUNT 알림 발송
        incrementUnreadAndNotify(channelId, userId);

        // @AI 멘션 감지 → 비동기 AI 응답 처리
        if (request.getContent() != null && request.getContent().contains("@AI")) {
            String tempId = UUID.randomUUID().toString();
            aiMessageService.processAiMentionAsync(channelId, userId, request.getContent(), tempId);
        }
    }

    /**
     * 타이핑 인디케이터.
     * /app/channel/{channelId}/typing
     * 에페머럴이므로 Redis Pub/Sub 없이 직접 브로드캐스트 (SimpMessagingTemplate 미사용 → publisher 경유)
     */
    @MessageMapping("/channel/{channelId}/typing")
    public void typingIndicator(@DestinationVariable Long channelId,
                                 Principal principal) {
        Long userId = extractUserId(principal);

        String nickname = userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        ChatEventDto event = ChatEventDto.builder()
                .eventType("TYPING")
                .channelId(channelId)
                .userId(userId)
                .nickname(nickname)
                .build();

        // 타이핑은 에페머럴 → Redis 거치지 않고 직접 발행
        redisPublisher.publish("chat:channel:" + channelId, event);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    /**
     * 발신자 제외 채널 참여자의 안읽음 카운트 증가 및 UNREAD_COUNT 알림 발송.
     */
    private void incrementUnreadAndNotify(Long channelId, Long senderId) {
        List<ChannelParticipant> participants = channelParticipantRepository.findByChannelId(channelId);
        for (ChannelParticipant participant : participants) {
            if (participant.getUserId().equals(senderId)) continue;
            unreadCountRepository.increment(participant.getUserId(), channelId);
            long unreadCount = unreadCountRepository.getCount(participant.getUserId(), channelId);
            notificationService.sendChannelUnreadCount(participant.getUserId(), channelId, unreadCount);
        }
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
