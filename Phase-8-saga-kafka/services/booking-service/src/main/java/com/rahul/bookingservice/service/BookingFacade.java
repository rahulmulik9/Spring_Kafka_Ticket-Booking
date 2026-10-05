package com.rahul.bookingservice.service;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.exception.BookingAlreadyCancelledException;
import com.rahul.bookingservice.exception.InvalidBookingStateException;
import com.rahul.bookingservice.idempotency.BookingRequestHasher;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * NOT @Transactional on purpose. The duplicate-key exception must be caught OUTSIDE the transaction
 * that failed, because a failed transaction cannot be used any more.
 *
 * Known gaps, fixed in the next steps:
 *  - A message can arrive twice, because the outbox is at-least-once (Step 8).
 */
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final AuditService auditService;

    public Booking createBooking(Long showId, BookingRequest request, AuthUser user, String idempotencyKey) {
        try {
            Booking booking = doCreateBooking(showId, request, user, idempotencyKey);
            auditService.logAttempt(showId, user.getEmail(), true, null);
            return booking;
        } catch (RuntimeException ex) {
            auditService.logAttempt(showId, user.getEmail(), false, ex.getMessage());
            throw ex;
        }
    }

    private Booking doCreateBooking(Long showId, BookingRequest request, AuthUser user, String idempotencyKey) {
        String hash = BookingRequestHasher.hash(showId, request.getSeatIds());

        // 1. Seen this key before? Return the same booking (or refuse if the request differs).
        Optional<Booking> existing = bookingService.findReplay(user.getId(), idempotencyKey, hash);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. First time: create it. If two identical requests arrive together, both reach this line,
        //  and the unique constraint lets exactly one of them commit.
        try {
            return bookingService.createPendingBooking(showId, user, request.getSeatIds(), idempotencyKey, hash);
        } catch (DataIntegrityViolationException ex) {
            // We lost the race. The winner has committed by now, so its booking is visible.
            // If no key row exists, the violation came from something else, so we rethrow it.
            return bookingService.findReplay(user.getId(), idempotencyKey, hash).orElseThrow(() -> ex);
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