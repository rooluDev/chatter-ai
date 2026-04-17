package com.chatterai.reaction.dto;

import lombok.Getter;

@Getter
public class ReactionResponseDto {

    private final Long messageId;
    private final String emoji;
    private final int count;
    private final boolean isMyReaction;

    public ReactionResponseDto(Long messageId, String emoji, int count, boolean isMyReaction) {
        this.messageId = messageId;
        this.emoji = emoji;
        this.count = count;
        this.isMyReaction = isMyReaction;
    }
}
