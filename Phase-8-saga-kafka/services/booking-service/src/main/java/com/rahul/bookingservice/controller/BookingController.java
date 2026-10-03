package com.rahul.bookingservice.controller;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.dto.BookingResponse;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.mapper.BookingMapper;
import com.rahul.bookingservice.security.AuthUser;
import com.rahul.bookingservice.service.BookingFacade;
import com.rahul.bookingservice.service.BookingService;
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
        Booking booking = bookingFacade.cancelBooking(id, user);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }
}