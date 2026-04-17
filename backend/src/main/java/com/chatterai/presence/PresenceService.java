package com.chatterai.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis TTL 기반 온라인 상태 관리.
 * key: presence:{userId}, value: "ONLINE" | "AWAY", TTL: 60초
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final String KEY_PREFIX = "presence:";
    private static final long ONLINE_TTL_SECONDS = 60L;

    private final RedisTemplate<String, String> redisTemplate;

    /** 온라인 상태로 설정 (TTL 갱신). */
    public void setOnline(Long userId) {
        redisTemplate.opsForValue().set(key(userId), "ONLINE", ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 자리비움 상태로 설정 (TTL 갱신). */
    public void setAway(Long userId) {
        redisTemplate.opsForValue().set(key(userId), "AWAY", ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 오프라인 처리 (키 삭제). */
    public void setOffline(Long userId) {
        redisTemplate.delete(key(userId));
    }

    /** 상태 조회. null이면 오프라인. */
    public String getStatus(Long userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    /** 온라인 여부 (ONLINE 또는 AWAY 모두 true). */
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
