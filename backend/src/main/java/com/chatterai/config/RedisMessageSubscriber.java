package com.chatterai.config;

import com.chatterai.message.dto.ChatEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Pub/Sub 수신 → STOMP 브로드캐스트.
     *
     * @param message      JSON 직렬화된 ChatEventDto
     * @param redisChannel Redis 채널명 (예: chat:channel:1, chat:dm:5)
     */
    public void handleMessage(String message, String redisChannel) {
        try {
            ChatEventDto event = objectMapper.readValue(message, ChatEventDto.class);
            String stompTopic = resolveStompTopic(redisChannel);
            if (stompTopic != null) {
                messagingTemplate.convertAndSend(stompTopic, event);
                log.debug("Broadcast to {}: eventType={}", stompTopic, event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process Redis message: channel={}", redisChannel, e);
        }
    }

    /**
     * Redis 채널명 → STOMP 토픽 변환.
     * chat:channel:{id} → /topic/channel/{id}
     * chat:dm:{id}      → /topic/dm/{id}
     */
    private String resolveStompTopic(String redisChannel) {
        if (redisChannel.startsWith("chat:channel:")) {
            String channelId = redisChannel.substring("chat:channel:".length());
            return "/topic/channel/" + channelId;
        }
        if (redisChannel.startsWith("chat:dm:")) {
            String dmRoomId = redisChannel.substring("chat:dm:".length());
            return "/topic/dm/" + dmRoomId;
        }
        log.warn("Unknown Redis channel pattern: {}", redisChannel);
        return null;
    }
}
