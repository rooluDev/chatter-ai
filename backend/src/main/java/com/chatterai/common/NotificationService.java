package com.chatterai.common;

import com.chatterai.message.dto.ChatEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 사용자 개별 알림 전송 서비스.
 * 채널/DM 브로드캐스트와 달리 특정 사용자에게만 전달하므로
 * Redis Pub/Sub 대신 SimpMessagingTemplate.convertAndSendToUser()를 사용한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String NOTIFICATION_DESTINATION = "/queue/notification";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채널 안읽은 수 알림.
     * → /user/{userId}/queue/notification
     */
    public void sendChannelUnreadCount(Long targetUserId, Long channelId, long unreadCount) {
        ChatEventDto event = ChatEventDto.builder()
                .eventType("UNREAD_COUNT")
                .channelId(channelId)
                .unreadCount(unreadCount)
                .build();
        messagingTemplate.convertAndSendToUser(
                targetUserId.toString(), NOTIFICATION_DESTINATION, event);
    }

    /**
     * DM 안읽은 수 알림.
     * → /user/{userId}/queue/notification
     */
    public void sendDmUnreadCount(Long targetUserId, Long dmRoomId, long unreadCount) {
        ChatEventDto event = ChatEventDto.builder()
                .eventType("UNREAD_COUNT")
                .dmRoomId(dmRoomId)
                .unreadCount(unreadCount)
                .build();
        messagingTemplate.convertAndSendToUser(
                targetUserId.toString(), NOTIFICATION_DESTINATION, event);
    }
}
