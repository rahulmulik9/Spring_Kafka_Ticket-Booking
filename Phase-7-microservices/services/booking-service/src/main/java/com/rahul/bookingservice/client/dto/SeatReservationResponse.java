package com.rahul.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationResponse {

    private Long showId;
    private String movieName;
    private LocalDateTime showTime;
    private List<ReservedSeat> seats;
}