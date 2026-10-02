package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import com.rahul.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// NOT @Transactional on purpose: the service's own transaction must really commit,
// so we can see whether the declined record survives.
@SpringBootTest
class DeclinedPaymentCommitTest {

    private static final long BOOKING_ID = 900030L;

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll(paymentRepository.findByBookingIdOrderByIdDesc(BOOKING_ID));
    }

    @Test
    void declinedPaymentIsCommittedNotRolledBack() {
        try {
            paymentService.pay(BOOKING_ID, 7L, new BigDecimal("1250.00"));
        } catch (RuntimeException ignored) {
            // the service threw, as in the experiment
        }

        List<Payment> stored = paymentRepository.findByBookingIdOrderByIdDesc(BOOKING_ID);

        assertEquals(1, stored.size());
    }
}