package com.chatterai.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis 채널에 메시지 발행.
     * @param redisChannel Redis pub/sub 채널명 (예: chat:channel:1, chat:dm:5)
     * @param message      브로드캐스트할 객체 (JSON 직렬화됨)
     */
    public void publish(String redisChannel, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(redisChannel, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for Redis publish: channel={}", redisChannel, e);
        }
    }
}
