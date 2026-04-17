package com.chatterai.reaction.controller;

import com.chatterai.common.ApiResponse;
import com.chatterai.common.SecurityUtil;
import com.chatterai.reaction.dto.ReactionRequestDto;
import com.chatterai.reaction.dto.ReactionResponseDto;
import com.chatterai.reaction.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages/{messageId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    /**
     * POST /api/messages/{messageId}/reactions
     * 이모지 반응 추가.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReactionResponseDto>> addReaction(
            @PathVariable Long messageId,
            @RequestBody @Valid ReactionRequestDto request) {

        Long userId = SecurityUtil.getCurrentUserId();
        ReactionResponseDto response = reactionService.addReaction(messageId, userId, request.getEmoji());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("반응이 추가되었습니다.", response));
    }

    /**
     * DELETE /api/messages/{messageId}/reactions
     * 이모지 반응 취소.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<ReactionResponseDto>> removeReaction(
            @PathVariable Long messageId,
            @RequestBody @Valid ReactionRequestDto request) {

        Long userId = SecurityUtil.getCurrentUserId();
        ReactionResponseDto response = reactionService.removeReaction(messageId, userId, request.getEmoji());
        return ResponseEntity.ok(ApiResponse.success("반응이 취소되었습니다.", response));
    }
}
