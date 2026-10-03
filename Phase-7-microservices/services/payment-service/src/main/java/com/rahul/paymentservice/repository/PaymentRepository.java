package com.rahul.paymentservice.repository;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // All attempts for a booking, newest first.
    List<Payment> findByBookingIdOrderByIdDesc(Long bookingId);

    // "Has this booking already been paid?" Runs as a cheap existence check, no rows are loaded.
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}