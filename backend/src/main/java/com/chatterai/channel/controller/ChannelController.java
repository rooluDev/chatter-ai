package com.chatterai.channel.controller;

import com.chatterai.channel.dto.ChannelDetailResponseDto;
import com.chatterai.channel.dto.ChannelResponseDto;
import com.chatterai.channel.service.ChannelService;
import com.chatterai.common.ApiResponse;
import com.chatterai.common.CursorPageResponse;
import com.chatterai.common.SecurityUtil;
import com.chatterai.message.dto.MessageResponseDto;
import com.chatterai.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final MessageService messageService;

    // 참여 채널 목록
    @GetMapping
    public ApiResponse<List<ChannelResponseDto>> getMyChannels() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success("조회되었습니다.", channelService.getMyChannels(userId));
    }

    // 채널 상세
    @GetMapping("/{channelId}")
    public ApiResponse<ChannelDetailResponseDto> getChannelDetail(@PathVariable Long channelId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success("조회되었습니다.", channelService.getChannelDetail(channelId, userId));
    }

    // 채널 메시지 목록 (커서 기반)
    @GetMapping("/{channelId}/messages")
    public ApiResponse<CursorPageResponse<MessageResponseDto>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long before) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success("조회되었습니다.",
                messageService.getChannelMessages(channelId, userId, before, size));
    }

    // 읽음 처리
    @DeleteMapping("/{channelId}/unread")
    public ApiResponse<Void> markAsRead(@PathVariable Long channelId) {
        Long userId = SecurityUtil.getCurrentUserId();
        channelService.markAsRead(channelId, userId);
        return ApiResponse.success("읽음 처리되었습니다.");
    }

    // 채널 참여 (신규 가입 사용자 또는 직접 참여)
    @PostMapping("/{channelId}/join")
    public ApiResponse<Void> joinChannel(@PathVariable Long channelId) {
        Long userId = SecurityUtil.getCurrentUserId();
        channelService.joinChannel(channelId, userId);
        return ApiResponse.success("채널에 참여했습니다.");
    }
}
