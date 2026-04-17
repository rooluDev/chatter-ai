package com.chatterai.message.service;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.attachment.repository.AttachmentRepository;
import com.chatterai.channel.repository.ChannelParticipantRepository;
import com.chatterai.channel.repository.ChannelRepository;
import com.chatterai.common.CursorPageResponse;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.message.dto.MessageResponseDto;
import com.chatterai.message.dto.MessageUpdateRequestDto;
import com.chatterai.message.entity.Message;
import com.chatterai.message.repository.MessageRepository;
import com.chatterai.reaction.entity.Reaction;
import com.chatterai.reaction.repository.ReactionRepository;
import com.chatterai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int MAX_SIZE = 50;

    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ChannelParticipantRepository channelParticipantRepository;

    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponseDto> getChannelMessages(
            Long channelId, Long userId, Long beforeId, int size) {

        channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        int fetchSize = Math.min(size, MAX_SIZE);
        // size+1 조회로 hasNext 판별
        PageRequest pageable = PageRequest.of(0, fetchSize + 1);

        List<Message> messages = beforeId == null
                ? messageRepository.findByChannelIdOrderByIdDesc(channelId, pageable)
                : messageRepository.findByChannelIdBeforeIdOrderByIdDesc(channelId, beforeId, pageable);

        boolean hasNext = messages.size() > fetchSize;
        if (hasNext) messages = messages.subList(0, fetchSize);

        // DESC → ASC 순서로 뒤집어 클라이언트에 전달 (오래된 메시지가 위)
        List<Message> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);

        Long nextCursorId = hasNext ? messages.get(messages.size() - 1).getId() : null;

        return new CursorPageResponse<>(
                buildMessageDtos(ordered, userId),
                hasNext,
                nextCursorId
        );
    }

    @Transactional
    public MessageResponseDto updateMessage(Long messageId, Long userId,
                                            MessageUpdateRequestDto request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_MESSAGE);
        }
        if (message.isAiMessage()) {
            throw new CustomException(ErrorCode.AI_MESSAGE_NOT_EDITABLE);
        }
        if (!message.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        message.updateContent(request.getContent());
        messageRepository.save(message);

        return buildSingleMessageDto(message, userId);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_MESSAGE);
        }
        if (message.getUserId() == null || !message.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        message.softDelete();
        messageRepository.save(message);
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────

    private List<MessageResponseDto> buildMessageDtos(List<Message> messages, Long currentUserId) {
        if (messages.isEmpty()) return Collections.emptyList();

        List<Long> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());

        // 첨부파일 일괄 조회
        Map<Long, List<Attachment>> attachmentsByMsgId = attachmentRepository
                .findByMessageIdInOrderBySortOrderAsc(messageIds)
                .stream()
                .collect(Collectors.groupingBy(Attachment::getMessageId));

        // 이모지 반응 일괄 조회
        Map<Long, List<Reaction>> reactionsByMsgId = reactionRepository
                .findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(Reaction::getMessageId));

        // 닉네임 일괄 조회
        Set<Long> userIds = messages.stream()
                .filter(m -> m.getUserId() != null)
                .map(Message::getUserId)
                .collect(Collectors.toSet());

        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));

        return messages.stream().map(message -> new MessageResponseDto(
                message,
                message.getUserId() != null ? nicknameMap.getOrDefault(message.getUserId(), "알 수 없음") : null,
                currentUserId,
                attachmentsByMsgId.getOrDefault(message.getId(), Collections.emptyList()),
                reactionsByMsgId.getOrDefault(message.getId(), Collections.emptyList())
        )).collect(Collectors.toList());
    }

    private MessageResponseDto buildSingleMessageDto(Message message, Long currentUserId) {
        List<Attachment> attachments = attachmentRepository
                .findByMessageIdInOrderBySortOrderAsc(List.of(message.getId()));
        List<Reaction> reactions = reactionRepository
                .findByMessageIdIn(List.of(message.getId()));

        String nickname = null;
        if (message.getUserId() != null) {
            nickname = userRepository.findById(message.getUserId())
                    .map(u -> u.getNickname())
                    .orElse("알 수 없음");
        }

        return new MessageResponseDto(message, nickname, currentUserId, attachments, reactions);
    }
}
