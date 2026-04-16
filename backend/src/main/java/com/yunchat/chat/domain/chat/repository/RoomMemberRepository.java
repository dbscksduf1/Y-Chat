package com.yunchat.chat.domain.chat.repository;

import com.yunchat.chat.domain.chat.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByUsername(String username);

    List<RoomMember> findByRoomId(Long roomId);

    @Query("SELECT rm FROM RoomMember rm JOIN FETCH rm.room WHERE rm.room.id IN :roomIds")
    List<RoomMember> findByRoomIds(@Param("roomIds") List<Long> roomIds);

    Optional<RoomMember> findByRoomIdAndUsername(Long roomId, String username);

    boolean existsByRoomIdAndUsername(Long roomId, String username);
}