package com.chatterai.auth.jwt;

import com.chatterai.user.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * STOMP CONNECT 프레임에서 JWT 검증 후 Principal 설정.
 * HTTP 필터 체인을 거치지 않는 WebSocket 연결의 인증 담당.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
                throw new MessageDeliveryException("UNAUTHORIZED: Missing token");
            }

            token = token.substring(7);

            if (!jwtProvider.validateToken(token)) {
                throw new MessageDeliveryException("UNAUTHORIZED: Invalid or expired token");
            }

            Long userId = jwtProvider.getUserIdFromToken(token);
            Role role = jwtProvider.getRoleFromToken(token);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            accessor.setUser(auth);
            log.debug("STOMP CONNECT authenticated: userId={}", userId);
        }

        return message;
    }
}
