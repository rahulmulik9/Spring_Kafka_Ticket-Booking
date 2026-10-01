package com.rahul.ticketbooking.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SeatHoldResponse {

    private Long showId;
    private List<Long> seatIds;
    private long expiresInSeconds;
}