package com.chatterai.reaction.service;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.attachment.repository.AttachmentRepository;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.config.RedisMessagePublisher;
import com.chatterai.message.dto.ChatEventDto;
import com.chatterai.message.dto.ChatMessageResponseDto;
import com.chatterai.message.entity.Message;
import com.chatterai.message.repository.MessageRepository;
import com.chatterai.reaction.dto.ReactionResponseDto;
import com.chatterai.reaction.entity.Reaction;
import com.chatterai.reaction.repository.ReactionRepository;
import com.chatterai.user.entity.User;
import com.chatterai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final RedisMessagePublisher redisPublisher;

    /**
     * 이모지 반응 추가.
     * UNIQUE 제약 위반 시 409 반환.
     * 성공 시 REACTION_UPDATED 브로드캐스트.
     */
    @Transactional
    public ReactionResponseDto addReaction(Long messageId, Long userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_MESSAGE);
        }

        if (reactionRepository.existsByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)) {
            throw new CustomException(ErrorCode.REACTION_ALREADY_EXISTS);
        }

        reactionRepository.save(Reaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build());

        List<Reaction> updatedReactions = reactionRepository.findByMessageIdIn(List.of(messageId));
        long count = updatedReactions.stream().filter(r -> r.getEmoji().equals(emoji)).count();

        broadcastReactionUpdated(message, userId);

        return new ReactionResponseDto(messageId, emoji, (int) count, true);
    }

    /**
     * 이모지 반응 취소.
     * 성공 시 REACTION_UPDATED 브로드캐스트.
     */
    @Transactional
    public ReactionResponseDto removeReaction(Long messageId, Long userId, String emoji) {
        Reaction reaction = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseThrow(() -> new CustomException(ErrorCode.REACTION_NOT_FOUND));

        reactionRepository.delete(reaction);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        List<Reaction> updatedReactions = reactionRepository.findByMessageIdIn(List.of(messageId));
        long count = updatedReactions.stream().filter(r -> r.getEmoji().equals(emoji)).count();

        broadcastReactionUpdated(message, userId);

        return new ReactionResponseDto(messageId, emoji, (int) count, false);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    private void broadcastReactionUpdated(Message message, Long currentUserId) {
        List<Reaction> allReactions = reactionRepository.findByMessageIdIn(List.of(message.getId()));
        List<Attachment> attachments = attachmentRepository
                .findByMessageIdInOrderBySortOrderAsc(List.of(message.getId()));

        String nickname = null;
        if (message.getUserId() != null) {
            nickname = userRepository.findById(message.getUserId())
                    .map(User::getNickname)
                    .orElse("알 수 없음");
        }

        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(
                message, nickname, attachments, allReactions);

        ChatEventDto event = ChatEventDto.builder()
                .eventType("REACTION_UPDATED")
                .message(msgDto)
                .build();

        // 채널 메시지 vs DM 메시지 구분
        if (message.getChannelId() != null) {
            redisPublisher.publish("chat:channel:" + message.getChannelId(), event);
        } else if (message.getDmRoomId() != null) {
            redisPublisher.publish("chat:dm:" + message.getDmRoomId(), event);
        }
    }
}
