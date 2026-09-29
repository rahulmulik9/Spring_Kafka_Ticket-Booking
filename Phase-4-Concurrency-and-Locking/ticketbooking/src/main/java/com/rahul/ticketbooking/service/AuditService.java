package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.BookingAttemptLog;
import com.rahul.ticketbooking.repository.BookingAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final BookingAttemptLogRepository bookingAttemptLogRepository;

    // REQUIRES_NEW: suspends the caller's transaction (if any) and starts a brand new one.
    // This log commits on its own, so it survives even if the calling transaction rolls back.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAttempt(Long showId, String customerEmail, boolean successful, String failureReason) {
        BookingAttemptLog logEntry = new BookingAttemptLog();
        logEntry.setShowId(showId);
        logEntry.setCustomerEmail(customerEmail);
        logEntry.setSuccessful(successful);
        logEntry.setFailureReason(failureReason);
        logEntry.setAttemptedAt(LocalDateTime.now());
        bookingAttemptLogRepository.save(logEntry);
    }
}