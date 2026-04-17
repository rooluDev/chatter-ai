package com.chatterai.message.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket STOMP 브로드캐스트 이벤트 DTO.
 * eventType에 따라 사용되는 필드가 다르므로 @JsonInclude(NON_NULL)로 미사용 필드 제외.
 *
 * eventType 목록:
 *   CHAT, AI_LOADING, AI_RESPONSE, AI_ERROR,
 *   MESSAGE_UPDATED, MESSAGE_DELETED,
 *   REACTION_UPDATED, NOTICE_UPDATED, CHANNEL_DELETED, TYPING
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatEventDto {

    private String eventType;

    // CHAT, AI_RESPONSE, AI_ERROR, MESSAGE_UPDATED, REACTION_UPDATED
    private ChatMessageResponseDto message;

    // AI_LOADING, AI_RESPONSE, AI_ERROR
    private String tempId;

    // AI_LOADING, CHANNEL_DELETED, NOTICE_UPDATED
    private Long channelId;

    // AI_LOADING
    private String content;

    // MESSAGE_DELETED
    private Long messageId;

    // NOTICE_UPDATED
    private NoticeMessageInfo noticeMessage;

    // UNREAD_COUNT (채널)
    private Long unreadCount;

    // UNREAD_COUNT (DM)
    private Long dmRoomId;

    // TYPING
    private Long userId;
    private String nickname;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoticeMessageInfo {
        private Long id;
        private String content;
        private java.time.LocalDateTime createdAt;
    }
}
