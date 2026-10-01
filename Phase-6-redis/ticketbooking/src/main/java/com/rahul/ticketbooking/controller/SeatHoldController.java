package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.SeatHoldRequest;
import com.rahul.ticketbooking.dto.SeatHoldResponse;
import com.rahul.ticketbooking.security.AuthUser;
import com.rahul.ticketbooking.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holds")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping("/{showId}")
    public ResponseEntity<SeatHoldResponse> holdSeats(@PathVariable Long showId,
                                                      @Valid @RequestBody SeatHoldRequest request,
                                                      @AuthenticationPrincipal AuthUser user) {
        SeatHoldResponse response = seatHoldService.holdSeats(showId, request.getSeatIds(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseSeats(@Valid @RequestBody SeatHoldRequest request,
                                             @AuthenticationPrincipal AuthUser user) {
        seatHoldService.releaseSeats(request.getSeatIds(), user.getId());
        return ResponseEntity.noContent().build();
    }
}