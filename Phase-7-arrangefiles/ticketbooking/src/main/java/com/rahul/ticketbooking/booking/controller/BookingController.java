package com.rahul.ticketbooking.booking.controller;

import com.rahul.ticketbooking.booking.dto.BookingRequest;
import com.rahul.ticketbooking.booking.dto.BookingResponse;
import com.rahul.ticketbooking.booking.entity.Booking;
import com.rahul.ticketbooking.booking.mapper.BookingMapper;
import com.rahul.ticketbooking.auth.security.AuthUser;
import com.rahul.ticketbooking.booking.service.BookingFacade;
import com.rahul.ticketbooking.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingFacade bookingFacade;

    @PostMapping("/{showId}")
    public ResponseEntity<BookingResponse> createBooking(@PathVariable Long showId,
                                                         @Valid @RequestBody BookingRequest request,
                                                         @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingFacade.createBooking(showId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toResponse(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id,
                                                      @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingService.getBookingForUser(id, user);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id,
                                                         @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingService.cancelBooking(id, user);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }

    @PostMapping("/{showId}/redis-lock")
    public ResponseEntity<BookingResponse> createBookingRedisLock(@PathVariable Long showId,
                                                                  @Valid @RequestBody BookingRequest request,
                                                                  @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingFacade.createBookingRedisLock(showId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toResponse(booking));
    }
}