package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.idempotency.PaymentRequestHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

// NOT @Transactional on purpose: the duplicate-key exception must be caught outside the failed transaction.
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;

    public Payment pay(Long bookingId, Long userId, BigDecimal amount, String idempotencyKey) {
        String hash = PaymentRequestHasher.hash(bookingId, amount);

        Optional<Payment> existing = paymentService.findReplay(userId, idempotencyKey, hash);
        if (existing.isPresent()) {
            return existing.get();   // same request again: return the saved payment, charge nothing
        }

        try {
            return paymentService.payOnce(bookingId, userId, amount, idempotencyKey, hash);
        } catch (DataIntegrityViolationException ex) {
            // Lost the race against an identical request. Return the winner's payment.
            return paymentService.findReplay(userId, idempotencyKey, hash).orElseThrow(() -> ex);
        }
    }
}