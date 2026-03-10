package com.momok.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import com.momok.rooms.domain.VoteRoom;

public interface RoomRepository extends JpaRepository<VoteRoom, String> {
}
