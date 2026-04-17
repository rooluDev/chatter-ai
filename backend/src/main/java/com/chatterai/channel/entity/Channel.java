package com.chatterai.channel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    // 순환 FK 방지: messages 테이블을 직접 참조하지 않고 Long으로 보관
    @Column(name = "notice_message_id")
    private Long noticeMessageId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Channel(String name, String description, boolean isPrivate, Long createdBy) {
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
        this.createdBy = createdBy;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void setNoticeMessageId(Long noticeMessageId) {
        this.noticeMessageId = noticeMessageId;
    }
}
