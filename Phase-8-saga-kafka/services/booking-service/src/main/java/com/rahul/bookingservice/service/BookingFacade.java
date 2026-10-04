package com.rahul.bookingservice.service;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.exception.BookingAlreadyCancelledException;
import com.rahul.bookingservice.exception.InvalidBookingStateException;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
 * Step 6: the facade no longer talks to Kafka. BookingService saves the change and its event
 * together, and OutboxPublisher sends the event.
 *
 * Known gaps, fixed in the next steps:
 *  - The same request twice creates two bookings (Step 7, idempotency key).
 *  - A message can arrive twice, because the outbox is at-least-once (Step 8).
 *  - Cinema and Payment still publish directly and have the same gap.
 */
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final AuditService auditService;

    public Booking createBooking(Long showId, BookingRequest request, AuthUser user) {
        try {
            Booking booking = bookingService.createPendingBooking(showId, user, request.getSeatIds());
            auditService.logAttempt(showId, user.getEmail(), true, null);
            return booking;
        } catch (RuntimeException ex) {
            auditService.logAttempt(showId, user.getEmail(), false, ex.getMessage());
            throw ex;
        }
    }

    public Booking cancelBooking(Long bookingId, AuthUser user) {
        Booking booking = bookingService.getBookingForUser(bookingId, user);   // 404 or 403 here

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking " + bookingId + " is already cancelled");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Only confirmed bookings can be cancelled. This booking is " + booking.getStatus());
        }

        return bookingService.markCancelled(bookingId);
    }
}