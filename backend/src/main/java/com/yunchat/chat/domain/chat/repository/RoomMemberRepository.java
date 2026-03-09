package com.yunchat.chat.domain.chat.repository;

import com.yunchat.chat.domain.chat.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByUsername(String username);

    List<RoomMember> findByRoomId(Long roomId);

    Optional<RoomMember> findByRoomIdAndUsername(Long roomId, String username);

    boolean existsByRoomIdAndUsername(Long roomId, String username);
}