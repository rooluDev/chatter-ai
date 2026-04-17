package com.chatterai.user.controller;

import com.chatterai.common.ApiResponse;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.common.SecurityUtil;
import com.chatterai.config.AiProperties;
import com.chatterai.message.service.AiUsageRepository;
import com.chatterai.user.entity.User;
import com.chatterai.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AiUsageRepository aiUsageRepository;
    private final AiProperties aiProperties;
    private final UserRepository userRepository;

    /**
     * GET /api/users/me
     * 현재 로그인 사용자 정보 조회.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMe() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success("조회되었습니다.", new UserInfoResponse(user)));
    }

    /**
     * GET /api/users/search?nickname={keyword}
     * 닉네임으로 사용자 검색 (DM 상대 찾기).
     * 최대 20명 반환.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchUsers(
            @RequestParam String nickname) {
        List<UserSearchResponse> users = userRepository
                .findByNicknameContainingIgnoreCaseAndIsActiveTrue(nickname, PageRequest.of(0, 20))
                .stream()
                .map(UserSearchResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("검색되었습니다.", users));
    }

    /**
     * GET /api/users/me/ai-usage
     * 현재 사용자의 AI 일일 사용량 조회.
     */
    @GetMapping("/me/ai-usage")
    public ResponseEntity<ApiResponse<AiUsageResponse>> getMyAiUsage() {
        Long userId = SecurityUtil.getCurrentUserId();
        int usedCount = aiUsageRepository.getUsageCount(userId);
        int limitCount = aiProperties.getDailyUsageLimit();
        int remainCount = Math.max(0, limitCount - usedCount);
        LocalDateTime resetAt = aiUsageRepository.getResetAt();

        return ResponseEntity.ok(ApiResponse.success("AI 사용량 조회 성공",
                new AiUsageResponse(usedCount, limitCount, remainCount, resetAt)));
    }

    @Getter
    public static class UserInfoResponse {
        private final Long id;
        private final String username;
        private final String nickname;
        private final String role;

        public UserInfoResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.role = user.getRole().name();
        }
    }

    @Getter
    public static class UserSearchResponse {
        private final Long id;
        private final String nickname;

        public UserSearchResponse(User user) {
            this.id = user.getId();
            this.nickname = user.getNickname();
        }
    }

    @Getter
    public static class AiUsageResponse {
        private final int usedCount;
        private final int limitCount;
        private final int remainCount;
        private final LocalDateTime resetAt;

        public AiUsageResponse(int usedCount, int limitCount, int remainCount, LocalDateTime resetAt) {
            this.usedCount = usedCount;
            this.limitCount = limitCount;
            this.remainCount = remainCount;
            this.resetAt = resetAt;
        }
    }
}
