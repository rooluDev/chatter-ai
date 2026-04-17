package com.chatterai.dm.repository;

import com.chatterai.dm.entity.DmRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DmRoomRepository extends JpaRepository<DmRoom, Long> {

    /**
     * 두 사용자 사이에 이미 존재하는 DM 방 ID 조회.
     */
    @Query("SELECT dp1.dmRoomId FROM DmParticipant dp1 " +
           "WHERE dp1.userId = :userId1 " +
           "AND EXISTS (" +
           "  SELECT dp2 FROM DmParticipant dp2 " +
           "  WHERE dp2.dmRoomId = dp1.dmRoomId AND dp2.userId = :userId2" +
           ")")
    Optional<Long> findDmRoomIdBetweenUsers(@Param("userId1") Long userId1,
                                            @Param("userId2") Long userId2);
}
