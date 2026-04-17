package com.chatterai.dm.controller;

import com.chatterai.common.ApiResponse;
import com.chatterai.common.CursorPageResponse;
import com.chatterai.common.SecurityUtil;
import com.chatterai.dm.dto.DmCreateRequestDto;
import com.chatterai.dm.dto.DmRoomDetailResponseDto;
import com.chatterai.dm.dto.DmRoomListResponseDto;
import com.chatterai.dm.service.DmService;
import com.chatterai.message.dto.MessageResponseDto;
import com.chatterai.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm")
@RequiredArgsConstructor
public class DmController {

    private final DmService dmService;

    /**
     * GET /api/dm/rooms
     * 내 DM 방 목록 조회.
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<DmRoomListResponseDto>>> getMyDmRooms() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<DmRoomListResponseDto> rooms = dmService.getMyDmRooms(userId);
        return ResponseEntity.ok(ApiResponse.success("조회되었습니다.", rooms));
    }

    /**
     * POST /api/dm/rooms
     * DM 방 생성 또는 기존 방 반환.
     * 신규 생성: 201, 기존 반환: 200.
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<DmRoomDetailResponseDto>> getOrCreateDmRoom(
            @RequestBody @Valid DmCreateRequestDto request) {
        Long userId = SecurityUtil.getCurrentUserId();

        Object[] result = dmService.getOrCreateDmRoom(userId, request.getTargetUserId());
        boolean isNew = (boolean) result[0];
        Long dmRoomId = (Long) result[1];
        User opponent = (User) result[2];

        // 온라인 상태는 DmService 내부에서 체크하므로 별도 조회
        DmRoomDetailResponseDto dto = buildDetailDto(dmRoomId, opponent, userId);

        if (isNew) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("DM 방이 생성되었습니다.", dto));
        } else {
            return ResponseEntity.ok(ApiResponse.success("기존 DM 방으로 이동합니다.", dto));
        }
    }

    /**
     * GET /api/dm/rooms/{dmRoomId}/messages?size=20&before={messageId}
     * DM 메시지 목록 조회 (커서 기반 페이지네이션).
     */
    @GetMapping("/rooms/{dmRoomId}/messages")
    public ResponseEntity<ApiResponse<CursorPageResponse<MessageResponseDto>>> getDmMessages(
            @PathVariable Long dmRoomId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long before) {
        Long userId = SecurityUtil.getCurrentUserId();
        CursorPageResponse<MessageResponseDto> page = dmService.getDmMessages(dmRoomId, userId, before, size);
        return ResponseEntity.ok(ApiResponse.success("조회되었습니다.", page));
    }

    /**
     * DELETE /api/dm/rooms/{dmRoomId}/unread
     * DM 읽음 처리.
     */
    @DeleteMapping("/rooms/{dmRoomId}/unread")
    public ResponseEntity<ApiResponse<Void>> markDmAsRead(@PathVariable Long dmRoomId) {
        Long userId = SecurityUtil.getCurrentUserId();
        dmService.markDmAsRead(dmRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success("읽음 처리되었습니다.", null));
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    private DmRoomDetailResponseDto buildDetailDto(Long dmRoomId, User opponent, Long currentUserId) {
        boolean isOnline = dmService.isUserOnline(opponent.getId());
        return new DmRoomDetailResponseDto(dmRoomId, opponent, isOnline);
    }
}
