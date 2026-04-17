package com.chatterai.auth.jwt;

import com.chatterai.auth.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token Redis 저장소
 * key: refresh:{userId}, value: refreshToken(UUID)
 * TTL: jwt.refresh-token-expiry (초)
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(userId),
                refreshToken,
                jwtProperties.getRefreshTokenExpiry(),
                TimeUnit.SECONDS
        );
    }

    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
