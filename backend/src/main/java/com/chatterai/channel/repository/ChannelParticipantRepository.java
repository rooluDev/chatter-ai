package com.chatterai.channel.repository;

import com.chatterai.channel.entity.ChannelParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelParticipantRepository extends JpaRepository<ChannelParticipant, Long> {

    boolean existsByChannelIdAndUserId(Long channelId, Long userId);

    Optional<ChannelParticipant> findByChannelIdAndUserId(Long channelId, Long userId);

    List<ChannelParticipant> findByChannelId(Long channelId);

    long countByChannelId(Long channelId);
}
