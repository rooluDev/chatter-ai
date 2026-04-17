package com.chatterai.message.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "dm_room_id")
    private Long dmRoomId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "is_ai_message", nullable = false)
    private boolean isAiMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Message(Long channelId, Long dmRoomId, Long userId,
                   String content, MessageType type, boolean isAiMessage) {
        this.channelId = channelId;
        this.dmRoomId = dmRoomId;
        this.userId = userId;
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
        this.isAiMessage = isAiMessage;
        this.isDeleted = false;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
