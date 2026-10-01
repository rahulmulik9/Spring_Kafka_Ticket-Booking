package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * Phase 6, Step 6: booking now respects the Redis seat hold.
 *  - Before booking: the caller must hold every seat they want.
 *  - After booking is saved: the holds are removed (best effort).
 *  - The database lock from Phase 4 is still the final guard against double booking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private static final int MAX_ATTEMPTS = 3;

    private final BookingService bookingService;
    private final AuditService auditService;
    private final SeatHoldService seatHoldService;

    // BiFunction only takes two arguments, and we now pass three, so a tiny interface replaces it.
    private interface BookingCall {
        Booking call(Long showId, BookingRequest request, Long userId);
    }

    public Booking createBooking(Long showId, BookingRequest request, AuthUser user) {
        return attempt(showId, request, user, bookingService::createBooking);
    }

    public Booking createBookingOptimistic(Long showId, BookingRequest request, AuthUser user) {
        return attempt(showId, request, user, bookingService::createBookingOptimistic);
    }

    private Booking attempt(Long showId, BookingRequest request, AuthUser user, BookingCall bookingCall) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                // Step 6: the user must hold every seat before booking
                seatHoldService.assertHeldByUser(request.getSeatIds(), user.getId());

                Booking booking = bookingCall.call(showId, request, user.getId());
                auditService.logAttempt(showId, user.getEmail(), true, null);

                // Only after the booking is committed do we remove the holds
                releaseHoldsQuietly(request.getSeatIds(), user.getId());
                return booking;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Seat conflict for {} (attempt {}/{})",
                        user.getEmail(), attempt, MAX_ATTEMPTS);
                if (attempt == MAX_ATTEMPTS) {
                    auditService.logAttempt(showId, user.getEmail(), false, "Seat conflict");
                    throw ex;
                }
            } catch (RuntimeException ex) {
                auditService.logAttempt(showId, user.getEmail(), false, ex.getMessage());
                throw ex;
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    // The booking is already saved. If Redis fails here, we must not fail the customer's booking.
    // The holds will expire by themselves anyway.
    private void releaseHoldsQuietly(List<Long> seatIds, Long userId) {
        try {
            seatHoldService.releaseSeats(seatIds, userId);
        } catch (RuntimeException ex) {
            log.warn("Booking saved, but releasing holds failed. They will expire by TTL. Reason: {}",
                    ex.getMessage());
        }
    }
}