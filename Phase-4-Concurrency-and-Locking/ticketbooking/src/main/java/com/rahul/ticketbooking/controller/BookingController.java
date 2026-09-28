package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.mapper.BookingMapper;
import com.rahul.ticketbooking.service.BookingFacade;
import com.rahul.ticketbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingFacade bookingFacade;

    @PostMapping("/{showId}")
    public ResponseEntity<BookingResponse> createBooking(@PathVariable Long showId,
                                                         @Valid @RequestBody BookingRequest request) {
        Booking booking = bookingFacade.createBooking(showId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toResponse(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        Booking booking = bookingService.cancelBooking(id);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }
}