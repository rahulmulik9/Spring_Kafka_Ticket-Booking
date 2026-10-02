package com.rahul.bookingservice.service;

import com.rahul.bookingservice.entity.BookingAttemptLog;
import com.rahul.bookingservice.repository.BookingAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final BookingAttemptLogRepository bookingAttemptLogRepository;

    // REQUIRES_NEW: this log commits on its own, so it survives even if the caller's work fails.
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