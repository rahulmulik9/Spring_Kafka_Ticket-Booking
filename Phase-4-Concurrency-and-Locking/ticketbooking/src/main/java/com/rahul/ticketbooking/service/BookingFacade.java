package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/*
 * Step 4: Handle the conflict.
 *  - Retry the whole transaction when an optimistic lock fails (separate bean, so each
 *    attempt gets a fresh transaction through the Spring proxy).
 *  - Retry ONLY the lock exception. Never retry business errors.
 *
 * Step 6: Audit outside the transaction.
 *  - Success AND failure audits are written here, after BookingService's transaction has ended.
 *  - Before, the failure audit ran inside the transaction (REQUIRES_NEW), which needed a 2nd
 *    connection while the 1st connection and the seat lock were still held. With a small pool
 *    that starves the pool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private static final int MAX_ATTEMPTS = 3;

    private final BookingService bookingService;
    private final AuditService auditService;

    public Booking createBooking(Long showId, BookingRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Booking booking = bookingService.createBooking(showId, request);
                auditService.logAttempt(showId, request.getCustomerEmail(), true, null);
                return booking;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Seat conflict for {} (attempt {}/{})",
                        request.getCustomerEmail(), attempt, MAX_ATTEMPTS);
                if (attempt == MAX_ATTEMPTS) {
                    auditService.logAttempt(showId, request.getCustomerEmail(), false, "Seat conflict");
                    throw ex;
                }
            } catch (RuntimeException ex) {
                auditService.logAttempt(showId, request.getCustomerEmail(), false, ex.getMessage());
                throw ex;
            }
        }
        throw new IllegalStateException("Unreachable");
    }
}