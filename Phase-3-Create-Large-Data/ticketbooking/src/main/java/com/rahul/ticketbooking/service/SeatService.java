package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.repository.SeatRepository;
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