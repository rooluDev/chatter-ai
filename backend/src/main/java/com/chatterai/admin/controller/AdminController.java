package com.chatterai.admin.controller;

import com.chatterai.admin.dto.AdminChannelCreateRequestDto;
import com.chatterai.admin.dto.AdminChannelResponseDto;
import com.chatterai.admin.dto.AdminNoticeRequestDto;
import com.chatterai.admin.dto.AdminUserPageResponseDto;
import com.chatterai.admin.service.AdminService;
import com.chatterai.common.ApiResponse;
import com.chatterai.common.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── 채널 관리 ──────────────────────────────────────────────

    /**
     * POST /api/admin/channels
     * 채널 생성.
     */
    @PostMapping("/channels")
    public ResponseEntity<ApiResponse<AdminChannelResponseDto>> createChannel(
            @RequestBody @Valid AdminChannelCreateRequestDto request) {

        Long adminUserId = SecurityUtil.getCurrentUserId();
        AdminChannelResponseDto response = adminService.createChannel(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("채널이 생성되었습니다.", response));
    }

    /**
     * DELETE /api/admin/channels/{channelId}
     * 채널 삭제. 구독자에게 CHANNEL_DELETED 이벤트 브로드캐스트.
     */
    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable Long channelId) {
        adminService.deleteChannel(channelId);
        return ResponseEntity.ok(ApiResponse.success("채널이 삭제되었습니다.", null));
    }

    // ── 공지 관리 ──────────────────────────────────────────────

    /**
     * POST /api/admin/channels/{channelId}/notice
     * 공지 메시지 지정.
     */
    @PostMapping("/channels/{channelId}/notice")
    public ResponseEntity<ApiResponse<Void>> setNotice(
            @PathVariable Long channelId,
            @RequestBody @Valid AdminNoticeRequestDto request) {

        adminService.setNoticeMessage(channelId, request);
        return ResponseEntity.ok(ApiResponse.success("공지가 설정되었습니다.", null));
    }

    /**
     * DELETE /api/admin/channels/{channelId}/notice
     * 공지 메시지 해제.
     */
    @DeleteMapping("/channels/{channelId}/notice")
    public ResponseEntity<ApiResponse<Void>> removeNotice(@PathVariable Long channelId) {
        adminService.removeNoticeMessage(channelId);
        return ResponseEntity.ok(ApiResponse.success("공지가 해제되었습니다.", null));
    }

    // ── 회원 관리 ──────────────────────────────────────────────

    /**
     * GET /api/admin/users?page=1&size=20&keyword={nickname}
     * 회원 목록 조회.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserPageResponseDto>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        AdminUserPageResponseDto response = adminService.getUsers(page, size, keyword);
        return ResponseEntity.ok(ApiResponse.success("조회되었습니다.", response));
    }

    /**
     * PUT /api/admin/users/{userId}/disable
     * 회원 비활성화.
     */
    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long userId) {
        Long adminUserId = SecurityUtil.getCurrentUserId();
        adminService.disableUser(userId, adminUserId);
        return ResponseEntity.ok(ApiResponse.success("계정이 비활성화되었습니다.", null));
    }

    /**
     * PUT /api/admin/users/{userId}/enable
     * 회원 활성화.
     */
    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long userId) {
        adminService.enableUser(userId);
        return ResponseEntity.ok(ApiResponse.success("계정이 활성화되었습니다.", null));
    }
}
