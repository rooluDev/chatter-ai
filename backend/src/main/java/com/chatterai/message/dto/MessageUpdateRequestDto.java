package com.chatterai.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class MessageUpdateRequestDto {

    @NotBlank(message = "메시지 내용을 입력해 주세요.")
    @Size(max = 4000, message = "메시지는 4000자를 초과할 수 없습니다.")
    private String content;
}
