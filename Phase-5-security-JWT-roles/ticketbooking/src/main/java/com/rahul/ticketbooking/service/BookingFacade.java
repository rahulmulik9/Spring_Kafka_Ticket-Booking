package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/*
 * Phase 5, Step 7: the caller now comes from the JWT (AuthUser), not from the request body.
 *  - The email in audit and log lines is the logged-in user's email.
 *  - The service receives only the user id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private static final int MAX_ATTEMPTS = 3;

    private final BookingService bookingService;
    private final AuditService auditService;

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
                Booking booking = bookingCall.call(showId, request, user.getId());
                auditService.logAttempt(showId, user.getEmail(), true, null);
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
}