package com.chatterai.dm.repository;

import com.chatterai.dm.entity.DmParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DmParticipantRepository extends JpaRepository<DmParticipant, Long> {

    List<DmParticipant> findByUserId(Long userId);

    List<DmParticipant> findByDmRoomId(Long dmRoomId);

    Optional<DmParticipant> findByDmRoomIdAndUserId(Long dmRoomId, Long userId);

    boolean existsByDmRoomIdAndUserId(Long dmRoomId, Long userId);
}
