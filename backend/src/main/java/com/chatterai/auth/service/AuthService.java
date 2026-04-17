package com.chatterai.auth.service;

import com.chatterai.auth.dto.JoinRequestDto;
import com.chatterai.auth.dto.LoginRequestDto;
import com.chatterai.auth.dto.LoginResponseDto;
import com.chatterai.auth.dto.TokenRefreshResponseDto;
import com.chatterai.auth.jwt.JwtProvider;
import com.chatterai.auth.jwt.RefreshTokenRepository;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.user.entity.User;
import com.chatterai.user.enums.Role;
import com.chatterai.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_COOKIE_NAME = "refresh";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void join(JoinRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request, HttpServletResponse response) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        if (!user.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRepository.save(user.getId(), refreshToken);

        addRefreshCookie(response, refreshToken);

        return new LoginResponseDto(accessToken, user);
    }

    @Transactional(readOnly = true)
    public TokenRefreshResponseDto refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        // refreshToken을 Redis에서 찾으려면 userId가 필요하다.
        // 모든 사용자의 키를 순회하는 대신, refresh 토큰 자체를 키로 사용하는 역방향 조회를 피하기 위해
        // 현재 Access Token에서 userId를 추출한다. (만료된 토큰에서도 파싱 가능)
        // 단, 요청 헤더에 만료된 Access Token이 있어야 한다.
        String expiredAccessToken = resolveAccessToken(request);
        if (expiredAccessToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId;
        try {
            userId = jwtProvider.getUserIdFromToken(expiredAccessToken);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String storedToken = refreshTokenRepository.find(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED));

        if (!storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 새 토큰 발급 (Refresh Token Rotation)
        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRepository.save(user.getId(), newRefreshToken);
        addRefreshCookie(response, newRefreshToken);

        return new TokenRefreshResponseDto(newAccessToken);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, Long userId) {
        refreshTokenRepository.delete(userId);
        deleteRefreshCookie(response);
    }

    // ── Cookie 유틸 ──────────────────────────────────────────────

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        // 운영환경에서는 Secure 활성화 필요 (HTTPS)
        response.addCookie(cookie);
    }

    private void deleteRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private java.util.Optional<String> extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return java.util.Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
