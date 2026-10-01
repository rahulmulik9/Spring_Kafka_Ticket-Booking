package com.rahul.ticketbooking.booking.repository;

import com.rahul.ticketbooking.booking.entity.BookingAttemptLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAttemptLogRepository extends JpaRepository<BookingAttemptLog, Long> {
}