package com.chatterai.channel.dto;

import com.chatterai.channel.entity.Channel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChannelResponseDto {

    private final Long id;
    private final String name;
    private final String description;
    private final boolean isPrivate;
    private final long unreadCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final LastMessage lastMessage;

    public ChannelResponseDto(Channel channel, long unreadCount, LastMessage lastMessage) {
        this.id = channel.getId();
        this.name = channel.getName();
        this.description = channel.getDescription();
        this.isPrivate = channel.isPrivate();
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
    }

    @Getter
    public static class LastMessage {
        private final String content;
        private final LocalDateTime createdAt;

        public LastMessage(String content, LocalDateTime createdAt) {
            this.content = content;
            this.createdAt = createdAt;
        }
    }
}
