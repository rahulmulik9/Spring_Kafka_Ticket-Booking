package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.BookingAttemptLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAttemptLogRepository extends JpaRepository<BookingAttemptLog, Long> {
}