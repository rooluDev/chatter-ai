package com.chatterai.message.dto;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.message.entity.Message;
import com.chatterai.reaction.entity.Reaction;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class MessageResponseDto {

    private static final String DELETED_CONTENT = "삭제된 메시지입니다.";
    private static final String AI_NICKNAME = "ChatterAI";

    private final Long id;
    private final Long userId;
    private final String nickname;
    private final String content;
    private final String type;
    private final boolean isDeleted;
    private final boolean isAiMessage;
    private final boolean isOwn;
    private final List<AttachmentDto> attachments;
    private final List<ReactionDto> reactions;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MessageResponseDto(Message message,
                               String nickname,
                               Long currentUserId,
                               List<Attachment> attachments,
                               List<Reaction> reactions) {
        this.id = message.getId();
        this.userId = message.getUserId();
        this.nickname = message.isAiMessage() ? AI_NICKNAME : nickname;
        this.isDeleted = message.isDeleted();
        this.isAiMessage = message.isAiMessage();
        this.isOwn = !message.isAiMessage()
                && message.getUserId() != null
                && message.getUserId().equals(currentUserId);
        this.type = message.getType().name();
        this.createdAt = message.getCreatedAt();
        this.updatedAt = message.getUpdatedAt();

        if (message.isDeleted()) {
            this.content = DELETED_CONTENT;
            this.attachments = Collections.emptyList();
            this.reactions = Collections.emptyList();
        } else {
            this.content = message.getContent();
            this.attachments = attachments.stream()
                    .map(AttachmentDto::new)
                    .collect(Collectors.toList());
            this.reactions = buildReactionDtos(reactions, currentUserId);
        }
    }

    private List<ReactionDto> buildReactionDtos(List<Reaction> reactions, Long currentUserId) {
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        return grouped.entrySet().stream()
                .map(entry -> new ReactionDto(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().anyMatch(r -> r.getUserId().equals(currentUserId))
                ))
                .collect(Collectors.toList());
    }

    @Getter
    public static class AttachmentDto {
        private final Long id;
        private final String fileUrl;
        private final String fileName;
        private final Long fileSize;
        private final String fileType;
        private final int sortOrder;

        public AttachmentDto(Attachment attachment) {
            this.id = attachment.getId();
            this.fileUrl = attachment.getFileUrl();
            this.fileName = attachment.getFileName();
            this.fileSize = attachment.getFileSize();
            this.fileType = attachment.getFileType().name();
            this.sortOrder = attachment.getSortOrder();
        }
    }

    @Getter
    public static class ReactionDto {
        private final String emoji;
        private final int count;
        private final boolean isMyReaction;

        public ReactionDto(String emoji, int count, boolean isMyReaction) {
            this.emoji = emoji;
            this.count = count;
            this.isMyReaction = isMyReaction;
        }
    }
}
