package com.chatterai.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {}

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 반환한다.
     * JwtAuthenticationFilter가 Authentication principal에 userId(Long)를 설정한다.
     *
     * @return 현재 사용자 ID
     * @throws CustomException UNAUTHORIZED — 인증 정보가 없을 때
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}
