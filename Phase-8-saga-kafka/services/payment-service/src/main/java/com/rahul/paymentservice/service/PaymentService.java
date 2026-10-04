package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import com.rahul.paymentservice.exception.PaymentAlreadyDoneException;
import com.rahul.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BigDecimal maxAmount;

    public PaymentService(PaymentRepository paymentRepository,
                          @Value("${payment.max-amount}") BigDecimal maxAmount) {
        this.paymentRepository = paymentRepository;
        this.maxAmount = maxAmount;
    }

    // A declined payment is a normal outcome, so it is RETURNED with status FAILED, not thrown.
    // If we threw here, the transaction would roll back and the FAILED record would vanish.
    @Transactional
    public Payment pay(Long bookingId, Long userId, BigDecimal amount) {
        if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.SUCCESS)) {
            throw new PaymentAlreadyDoneException("Booking " + bookingId + " is already paid");
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);

        if (amount.compareTo(maxAmount) > 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Amount exceeds the allowed limit of " + maxAmount);
        } else {
            payment.setStatus(PaymentStatus.SUCCESS);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} for booking {}: {}", saved.getId(), bookingId, saved.getStatus());
        return saved;   // the caller checks the status
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdDesc(bookingId);
    }

    // A customer sees only their own payments. An admin sees all of them.
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForCaller(Long bookingId, Long callerId, boolean isAdmin) {
        List<Payment> payments = paymentRepository.findByBookingIdOrderByIdDesc(bookingId);
        if (isAdmin) {
            return payments;
        }
        return payments.stream()
                .filter(p -> p.getUserId().equals(callerId))
                .toList();
    }
}