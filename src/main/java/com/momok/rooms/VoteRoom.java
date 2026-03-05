package com.momok.rooms;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.momok.rooms.Dto.VoteRoomResponseDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vote_room")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class VoteRoom {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@CreatedDate
	@Column(name = "vote_deadline", nullable = false)
	private LocalDateTime voteDeadline;

	@Column(name = "vote_longitude", nullable = false)
	private double longitude;

	@Column(name = "vote_latitude", nullable = false)
	private double latitude;

	@Column(name = "vote_password")
	private Integer password;

	public VoteRoomResponseDto toDto() {
		return VoteRoomResponseDto.builder().roomUrl("https://momok.site/rooms/" + this.id).build();
	}
}
