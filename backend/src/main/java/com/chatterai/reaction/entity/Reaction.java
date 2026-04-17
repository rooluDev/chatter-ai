package com.chatterai.reaction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "emoji"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String emoji;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Reaction(Long messageId, Long userId, String emoji) {
        this.messageId = messageId;
        this.userId = userId;
        this.emoji = emoji;
    }
}
