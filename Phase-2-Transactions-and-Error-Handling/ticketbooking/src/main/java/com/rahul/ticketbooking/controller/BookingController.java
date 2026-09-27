package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/shows/{showId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public Booking createBooking(@PathVariable Long showId, @RequestBody BookingRequest request) {
        return bookingService.createBooking(showId, request);
    }

    @GetMapping("/bookings/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @PatchMapping("/bookings/{id}/cancel")
    public Booking cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
    }
}