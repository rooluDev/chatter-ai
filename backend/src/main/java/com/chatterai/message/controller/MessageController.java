package com.chatterai.message.controller;

import com.chatterai.common.ApiResponse;
import com.chatterai.common.SecurityUtil;
import com.chatterai.message.dto.MessageResponseDto;
import com.chatterai.message.dto.MessageUpdateRequestDto;
import com.chatterai.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 메시지 수정
    @PutMapping("/{messageId}")
    public ApiResponse<MessageResponseDto> updateMessage(
            @PathVariable Long messageId,
            @RequestBody @Valid MessageUpdateRequestDto request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success("수정되었습니다.",
                messageService.updateMessage(messageId, userId, request));
    }

    // 메시지 삭제 (소프트)
    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long messageId) {
        Long userId = SecurityUtil.getCurrentUserId();
        messageService.deleteMessage(messageId, userId);
        return ApiResponse.success("삭제되었습니다.");
    }
}
