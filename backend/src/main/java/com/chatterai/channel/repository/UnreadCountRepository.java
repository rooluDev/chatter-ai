package com.chatterai.channel.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 안읽은 메시지 수 Redis 저장소
 * key: unread:{userId}:{channelId}, value: count (String)
 */
@Repository
@RequiredArgsConstructor
public class UnreadCountRepository {

    private static final String KEY_PREFIX = "unread:";
    private final RedisTemplate<String, String> redisTemplate;

    public void increment(Long userId, Long channelId) {
        redisTemplate.opsForValue().increment(key(userId, channelId));
    }

    public long getCount(Long userId, Long channelId) {
        String value = redisTemplate.opsForValue().get(key(userId, channelId));
        return value == null ? 0L : Long.parseLong(value);
    }

    public void reset(Long userId, Long channelId) {
        redisTemplate.delete(key(userId, channelId));
    }

    private String key(Long userId, Long channelId) {
        return KEY_PREFIX + userId + ":" + channelId;
    }
}
