package com.chatterai.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AdminChannelCreateRequestDto {

    @NotBlank(message = "채널 이름은 필수입니다.")
    @Size(min = 1, max = 30, message = "채널 이름은 1~30자여야 합니다.")
    private String name;

    @Size(max = 255, message = "설명은 255자 이하여야 합니다.")
    private String description;

    private boolean isPrivate;
}
