package com.chatterai.reaction.repository;

import com.chatterai.reaction.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    List<Reaction> findByMessageIdIn(List<Long> messageIds);

    Optional<Reaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    boolean existsByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);
}
