package com.rahul.ticketbooking.seat.service;

import com.rahul.ticketbooking.seat.entity.Seat;
import com.rahul.ticketbooking.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    public List<Seat> getSeatsByShow(Long showId) {
        return seatRepository.findByShowId(showId);
    }
}