package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import com.rahul.paymentservice.exception.PaymentAlreadyDoneException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Each test rolls back, so no data is left in paymentdb.
@SpringBootTest
@Transactional
class PaymentServiceTest {

    private static final long BOOKING_ID = 900020L;
    private static final long USER_ID = 7L;

    @Autowired
    private PaymentService paymentService;

    @Test
    void smallAmountSucceeds() {
        Payment payment = paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("250.00"));

        assertNotNull(payment.getId());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNull(payment.getFailureReason());
    }

    @Test
    void amountExactlyAtTheLimitSucceeds() {
        Payment payment = paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("1000.00"));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
    }

    @Test
    void amountOverTheLimitIsRecordedAsFailed() {
        Payment payment = paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("1250.00"));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNotNull(payment.getFailureReason());
        assertEquals(1, paymentService.getPaymentsByBooking(BOOKING_ID).size());
    }

    @Test
    void payingAgainAfterSuccessIsRejected() {
        paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("250.00"));

        assertThrows(PaymentAlreadyDoneException.class,
                () -> paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("250.00")));

        assertEquals(1, paymentService.getPaymentsByBooking(BOOKING_ID).size());
    }

    @Test
    void retryAfterAFailedAttemptIsAllowed() {
        paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("1250.00"));
        paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("250.00"));

        List<Payment> payments = paymentService.getPaymentsByBooking(BOOKING_ID);

        assertEquals(2, payments.size());
        assertEquals(PaymentStatus.SUCCESS, payments.get(0).getStatus());   // newest first
        assertEquals(PaymentStatus.FAILED, payments.get(1).getStatus());
    }

    @Test
    void callerSeesOnlyOwnPaymentsAndAdminSeesAll() {
        paymentService.pay(BOOKING_ID, USER_ID, new BigDecimal("250.00"));

        assertEquals(1, paymentService.getPaymentsForCaller(BOOKING_ID, USER_ID, false).size());
        assertEquals(0, paymentService.getPaymentsForCaller(BOOKING_ID, 99L, false).size());
        assertEquals(1, paymentService.getPaymentsForCaller(BOOKING_ID, 99L, true).size());
    }
}