package com.chatterai.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AdminNoticeRequestDto {

    @NotNull(message = "messageId는 필수입니다.")
    private Long messageId;
}
