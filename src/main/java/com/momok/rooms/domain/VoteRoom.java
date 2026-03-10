package com.momok.rooms.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;

import com.momok.rooms.Dto.VoteRoomDetailsResponseDto;
import com.momok.rooms.Dto.VoteRoomResponseDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vote_room")
@NoArgsConstructor
@Builder
@Getter
@Setter
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

	@Transient
	private List<RestaurantCard> restaurantCards;

	public VoteRoomDetailsResponseDto toDetailDto() {
		return VoteRoomDetailsResponseDto.builder()
			.roomId(id)
			.voteDeadline(voteDeadline)
			.password(password)
			.location(new VoteRoomDetailsResponseDto.Location(latitude, longitude))
			.restaurantCards(restaurantCards)
			.build();
	}

	public VoteRoomResponseDto toDto() {
		return VoteRoomResponseDto.builder()
			.roomId(id)
			.voteDeadline(voteDeadline)
			.location(new VoteRoomDetailsResponseDto.Location(latitude, longitude))
			.restaurantCards(restaurantCards)
			.build();
	}
}
