package com.momok.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<VoteRoom, String> {
}
