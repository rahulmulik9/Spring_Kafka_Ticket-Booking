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

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingFacade bookingFacade;

    // 202 Accepted: "we got your request and it is being processed". The status is PENDING for now.
    // The client must send a unique Idempotency-Key (a UUID is ideal, and the limit is 100 characters).
    @PostMapping("/{showId}")
    public ResponseEntity<BookingResponse> createBooking(@PathVariable Long showId,
                                                         @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                         @Valid @RequestBody BookingRequest request,
                                                         @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingFacade.createBooking(showId, request, user, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BookingMapper.toResponse(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id,
                                                      @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingService.getBookingForUser(id, user);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings(@AuthenticationPrincipal AuthUser user) {

        List<Booking> bookings = bookingService.getBookingsForUser(user);

        return ResponseEntity.ok(
                bookings.stream()
                        .map(BookingMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id,
                                                         @AuthenticationPrincipal AuthUser user) {
        Booking booking = bookingFacade.cancelBooking(id, user);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }
}