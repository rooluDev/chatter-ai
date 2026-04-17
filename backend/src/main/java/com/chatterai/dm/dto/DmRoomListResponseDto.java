package com.chatterai.dm.dto;

import com.chatterai.message.entity.Message;
import com.chatterai.user.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DmRoomListResponseDto {

    private final Long id;
    private final OpponentInfo opponent;
    private final long unreadCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final LastMessageInfo lastMessage;

    public DmRoomListResponseDto(Long dmRoomId, User opponent, boolean isOnline,
                                  long unreadCount, Message lastMsg) {
        this.id = dmRoomId;
        this.opponent = new OpponentInfo(opponent.getId(), opponent.getNickname(), isOnline);
        this.unreadCount = unreadCount;
        this.lastMessage = lastMsg != null ? new LastMessageInfo(lastMsg) : null;
    }

    @Getter
    public static class OpponentInfo {
        private final Long id;
        private final String nickname;
        private final boolean isOnline;

        public OpponentInfo(Long id, String nickname, boolean isOnline) {
            this.id = id;
            this.nickname = nickname;
            this.isOnline = isOnline;
        }
    }

    @Getter
    public static class LastMessageInfo {
        private final String content;
        private final LocalDateTime createdAt;

        public LastMessageInfo(Message message) {
            this.content = message.isDeleted() ? "삭제된 메시지입니다." : message.getContent();
            this.createdAt = message.getCreatedAt();
        }
    }
}
