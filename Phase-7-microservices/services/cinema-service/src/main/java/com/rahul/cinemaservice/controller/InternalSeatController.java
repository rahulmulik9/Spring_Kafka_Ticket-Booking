package com.rahul.cinemaservice.controller;

import com.rahul.cinemaservice.dto.SeatIdsRequest;
import com.rahul.cinemaservice.dto.SeatReservationResponse;
import com.rahul.cinemaservice.service.SeatReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Called by booking-service, not by customers. The Gateway has no route for /internal/**.
@RestController
@RequestMapping("/internal/shows/{showId}/seats")
@RequiredArgsConstructor
public class InternalSeatController {

    private final SeatReservationService seatReservationService;

    @PostMapping("/reserve")
    public SeatReservationResponse reserve(@PathVariable Long showId,
                                           @Valid @RequestBody SeatIdsRequest request) {
        return seatReservationService.reserveSeats(showId, request.getSeatIds());
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long showId,
                        @Valid @RequestBody SeatIdsRequest request) {
        seatReservationService.releaseSeats(showId, request.getSeatIds());
    }
}