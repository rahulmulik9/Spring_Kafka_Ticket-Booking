package com.rahul.paymentservice.repository;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Runs against the real paymentdb container. Each test rolls back, so no data is left behind.
@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    private static final long BOOKING_ID = 900001L;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment save(long bookingId, String amount, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(7L);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);
        return paymentRepository.saveAndFlush(payment);
    }

    @Test
    void savedPaymentGetsIdAndTimestamp() {
        Payment saved = save(BOOKING_ID, "250.00", PaymentStatus.SUCCESS);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void attemptsOfABookingComeBackNewestFirst() {
        Payment failed = save(BOOKING_ID, "250.00", PaymentStatus.FAILED);
        Payment success = save(BOOKING_ID, "250.00", PaymentStatus.SUCCESS);

        List<Payment> payments = paymentRepository.findByBookingIdOrderByIdDesc(BOOKING_ID);

        assertEquals(2, payments.size());
        assertEquals(success.getId(), payments.get(0).getId());
        assertEquals(failed.getId(), payments.get(1).getId());
    }

    @Test
    void otherBookingsAreNotMixedIn() {
        save(BOOKING_ID, "250.00", PaymentStatus.SUCCESS);
        save(900002L, "500.00", PaymentStatus.SUCCESS);

        assertEquals(1, paymentRepository.findByBookingIdOrderByIdDesc(BOOKING_ID).size());
    }

    @Test
    void existsChecksTheStatusToo() {
        save(BOOKING_ID, "250.00", PaymentStatus.FAILED);

        assertFalse(paymentRepository.existsByBookingIdAndStatus(BOOKING_ID, PaymentStatus.SUCCESS));

        save(BOOKING_ID, "250.00", PaymentStatus.SUCCESS);

        assertTrue(paymentRepository.existsByBookingIdAndStatus(BOOKING_ID, PaymentStatus.SUCCESS));
    }

    @Test
    void zeroAmountIsRefusedByTheDatabase() {
        assertThrows(DataIntegrityViolationException.class,
                () -> save(BOOKING_ID, "0.00", PaymentStatus.SUCCESS));
    }
}