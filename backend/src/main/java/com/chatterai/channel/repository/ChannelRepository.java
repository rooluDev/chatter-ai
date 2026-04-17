package com.chatterai.channel.repository;

import com.chatterai.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByName(String name);

    @Query("""
            SELECT c FROM Channel c
            JOIN ChannelParticipant cp ON cp.channelId = c.id
            WHERE cp.userId = :userId
            ORDER BY c.id ASC
            """)
    List<Channel> findByParticipantUserId(@Param("userId") Long userId);
}
