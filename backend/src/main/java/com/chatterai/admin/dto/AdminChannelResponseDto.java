package com.chatterai.admin.dto;

import com.chatterai.channel.entity.Channel;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminChannelResponseDto {

    private final Long id;
    private final String name;
    private final String description;
    private final boolean isPrivate;
    private final LocalDateTime createdAt;

    public AdminChannelResponseDto(Channel channel) {
        this.id = channel.getId();
        this.name = channel.getName();
        this.description = channel.getDescription();
        this.isPrivate = channel.isPrivate();
        this.createdAt = channel.getCreatedAt();
    }
}
