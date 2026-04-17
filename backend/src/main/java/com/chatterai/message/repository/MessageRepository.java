package com.chatterai.message.repository;

import com.chatterai.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 채널 최신 메시지 조회 (lastMessage용)
    Optional<Message> findTopByChannelIdAndIsDeletedFalseOrderByIdDesc(Long channelId);

    // 채널 커서 기반 페이지네이션 (before 없음 → 최신부터)
    @Query("SELECT m FROM Message m WHERE m.channelId = :channelId ORDER BY m.id DESC")
    List<Message> findByChannelIdOrderByIdDesc(@Param("channelId") Long channelId, Pageable pageable);

    // 채널 커서 기반 페이지네이션 (before 있음)
    @Query("SELECT m FROM Message m WHERE m.channelId = :channelId AND m.id < :beforeId ORDER BY m.id DESC")
    List<Message> findByChannelIdBeforeIdOrderByIdDesc(@Param("channelId") Long channelId,
                                                        @Param("beforeId") Long beforeId,
                                                        Pageable pageable);

    // DM 최신 메시지 조회 (lastMessage용)
    Optional<Message> findTopByDmRoomIdAndIsDeletedFalseOrderByIdDesc(Long dmRoomId);

    // DM 커서 기반 페이지네이션
    @Query("SELECT m FROM Message m WHERE m.dmRoomId = :dmRoomId ORDER BY m.id DESC")
    List<Message> findByDmRoomIdOrderByIdDesc(@Param("dmRoomId") Long dmRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.dmRoomId = :dmRoomId AND m.id < :beforeId ORDER BY m.id DESC")
    List<Message> findByDmRoomIdBeforeIdOrderByIdDesc(@Param("dmRoomId") Long dmRoomId,
                                                       @Param("beforeId") Long beforeId,
                                                       Pageable pageable);

    // AI 컨텍스트용 최근 N개 메시지 (채널)
    @Query("SELECT m FROM Message m WHERE m.channelId = :channelId AND m.isDeleted = false ORDER BY m.id DESC")
    List<Message> findRecentByChannelId(@Param("channelId") Long channelId, Pageable pageable);

    // AI 컨텍스트용 최근 N개 메시지 (DM)
    @Query("SELECT m FROM Message m WHERE m.dmRoomId = :dmRoomId AND m.isDeleted = false ORDER BY m.id DESC")
    List<Message> findRecentByDmRoomId(@Param("dmRoomId") Long dmRoomId, Pageable pageable);
}
