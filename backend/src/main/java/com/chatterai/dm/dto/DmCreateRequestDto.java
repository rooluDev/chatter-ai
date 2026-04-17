package com.chatterai.dm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class DmCreateRequestDto {

    @NotNull(message = "targetUserId는 필수입니다.")
    private Long targetUserId;
}
