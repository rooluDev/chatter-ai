package com.chatterai.auth.controller;

import com.chatterai.auth.dto.JoinRequestDto;
import com.chatterai.auth.dto.LoginRequestDto;
import com.chatterai.auth.dto.LoginResponseDto;
import com.chatterai.auth.dto.TokenRefreshResponseDto;
import com.chatterai.auth.service.AuthService;
import com.chatterai.common.ApiResponse;
import com.chatterai.common.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> join(@RequestBody @Valid JoinRequestDto request) {
        authService.join(request);
        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto request,
                                               HttpServletResponse response) {
        LoginResponseDto data = authService.login(request, response);
        return ApiResponse.success("로그인되었습니다.", data);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponseDto> refresh(HttpServletRequest request,
                                                        HttpServletResponse response) {
        TokenRefreshResponseDto data = authService.refresh(request, response);
        return ApiResponse.success("토큰이 갱신되었습니다.", data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.logout(request, response, userId);
        return ApiResponse.success("로그아웃되었습니다.");
    }
}
