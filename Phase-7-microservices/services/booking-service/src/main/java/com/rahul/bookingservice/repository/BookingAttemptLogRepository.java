package com.rahul.bookingservice.repository;

import com.rahul.bookingservice.entity.BookingAttemptLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAttemptLogRepository extends JpaRepository<BookingAttemptLog, Long> {
}