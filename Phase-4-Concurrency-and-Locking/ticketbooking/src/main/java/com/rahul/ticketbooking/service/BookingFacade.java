package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private static final int MAX_ATTEMPTS = 3;

    private final BookingService bookingService;
    private final AuditService auditService;

    // No @Transactional here. Each attempt must start its own new transaction.
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
            }
        }
        throw new IllegalStateException("Unreachable");
    }
}