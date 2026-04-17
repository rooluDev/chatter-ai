package com.chatterai.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Repository
@RequiredArgsConstructor
public class AiUsageRepository {

    private static final String KEY_PREFIX = "ai:usage:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 오늘 사용 횟수 조회.
     */
    public int getUsageCount(Long userId) {
        String key = buildKey(userId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     * 사용 횟수 1 증가. 키가 없으면 생성하고 자정까지 TTL 설정.
     */
    public int increment(Long userId) {
        String key = buildKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 첫 번째 호출 → 자정까지 TTL 설정
            redisTemplate.expire(key, ttlUntilMidnight());
        }
        return count == null ? 1 : count.intValue();
    }

    /**
     * 오늘 자정까지 남은 시간을 반환.
     */
    public LocalDateTime getResetAt() {
        return LocalDate.now().plusDays(1).atStartOfDay();
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now();
    }

    private Duration ttlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}
