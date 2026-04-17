package com.chatterai.message.dto;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.message.entity.Message;
import com.chatterai.reaction.entity.Reaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebSocket 브로드캐스트용 메시지 DTO.
 * REST MessageResponseDto와 달리 isOwn 필드 없음 (모든 구독자에게 동일한 데이터).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponseDto {

    private static final String DELETED_CONTENT = "삭제된 메시지입니다.";
    private static final String AI_NICKNAME = "ChatterAI";

    private Long id;
    private Long channelId;
    private Long dmRoomId;
    private Long userId;
    private String nickname;
    private String content;
    private String type;
    private boolean isDeleted;
    private boolean isAiMessage;
    private List<AttachmentInfo> attachments;
    private List<ReactionInfo> reactions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatMessageResponseDto of(Message message, String nickname,
                                            List<Attachment> attachments, List<Reaction> reactions) {
        ChatMessageResponseDtoBuilder builder = ChatMessageResponseDto.builder()
                .id(message.getId())
                .channelId(message.getChannelId())
                .dmRoomId(message.getDmRoomId())
                .userId(message.getUserId())
                .nickname(message.isAiMessage() ? AI_NICKNAME : nickname)
                .isDeleted(message.isDeleted())
                .isAiMessage(message.isAiMessage())
                .type(message.getType().name())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt());

        if (message.isDeleted()) {
            builder.content(DELETED_CONTENT)
                   .attachments(Collections.emptyList())
                   .reactions(Collections.emptyList());
        } else {
            builder.content(message.getContent())
                   .attachments(attachments.stream().map(AttachmentInfo::of).collect(Collectors.toList()))
                   .reactions(buildReactionInfos(reactions));
        }
        return builder.build();
    }

    private static List<ReactionInfo> buildReactionInfos(List<Reaction> reactions) {
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));
        return grouped.entrySet().stream()
                .map(e -> ReactionInfo.of(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().map(Reaction::getUserId).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Long id;
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private int sortOrder;

        public static AttachmentInfo of(Attachment a) {
            return AttachmentInfo.builder()
                    .id(a.getId())
                    .fileUrl(a.getFileUrl())
                    .fileName(a.getFileName())
                    .fileSize(a.getFileSize())
                    .fileType(a.getFileType().name())
                    .sortOrder(a.getSortOrder())
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionInfo {
        private String emoji;
        private int count;
        private List<Long> userIds;

        public static ReactionInfo of(String emoji, int count, List<Long> userIds) {
            return ReactionInfo.builder()
                    .emoji(emoji)
                    .count(count)
                    .userIds(userIds)
                    .build();
        }
    }
}
