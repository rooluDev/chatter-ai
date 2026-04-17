package com.chatterai.channel.dto;

import com.chatterai.channel.entity.Channel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChannelDetailResponseDto {

    private final Long id;
    private final String name;
    private final String description;
    private final boolean isPrivate;
    private final long participantCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final NoticeMessage noticeMessage;

    public ChannelDetailResponseDto(Channel channel, long participantCount, NoticeMessage noticeMessage) {
        this.id = channel.getId();
        this.name = channel.getName();
        this.description = channel.getDescription();
        this.isPrivate = channel.isPrivate();
        this.participantCount = participantCount;
        this.noticeMessage = noticeMessage;
    }

    @Getter
    public static class NoticeMessage {
        private final Long id;
        private final String content;
        private final LocalDateTime createdAt;

        public NoticeMessage(Long id, String content, LocalDateTime createdAt) {
            this.id = id;
            this.content = content;
            this.createdAt = createdAt;
        }
    }
}
