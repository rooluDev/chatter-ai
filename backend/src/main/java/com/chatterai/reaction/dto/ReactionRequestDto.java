package com.chatterai.reaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ReactionRequestDto {

    @NotBlank(message = "emoji는 필수입니다.")
    @Size(max = 10, message = "emoji는 10자 이하여야 합니다.")
    private String emoji;
}
