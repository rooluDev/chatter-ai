package com.chatterai.dm.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * DM 안읽은 메시지 수 Redis 저장소
 * key: dm_unread:{userId}:{dmRoomId}, value: count (String)
 */
@Repository
@RequiredArgsConstructor
public class DmUnreadCountRepository {

    private static final String KEY_PREFIX = "dm_unread:";
    private final RedisTemplate<String, String> redisTemplate;

    public void increment(Long userId, Long dmRoomId) {
        redisTemplate.opsForValue().increment(key(userId, dmRoomId));
    }

    public long getCount(Long userId, Long dmRoomId) {
        String value = redisTemplate.opsForValue().get(key(userId, dmRoomId));
        return value == null ? 0L : Long.parseLong(value);
    }

    public void reset(Long userId, Long dmRoomId) {
        redisTemplate.delete(key(userId, dmRoomId));
    }

    private String key(Long userId, Long dmRoomId) {
        return KEY_PREFIX + userId + ":" + dmRoomId;
    }
}
