package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import com.rahul.paymentservice.exception.PaymentAlreadyDoneException;
import com.rahul.paymentservice.idempotency.IdempotencyKey;
import com.rahul.paymentservice.idempotency.IdempotencyKeyRepository;
import com.rahul.paymentservice.idempotency.IdempotencyKeyReuseException;
import com.rahul.paymentservice.idempotency.ProcessedEventService;
import com.rahul.paymentservice.kafka.config.KafkaTopicConfig;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.event.SeatDetail;
import com.rahul.paymentservice.kafka.event.SeatsReservedEvent;
import com.rahul.paymentservice.outbox.OutboxService;
import com.rahul.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ProcessedEventService processedEventService;
    private final BigDecimal maxAmount;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxService outboxService,
                          IdempotencyKeyRepository idempotencyKeyRepository,
                          ProcessedEventService processedEventService,
                          @Value("${payment.max-amount}") BigDecimal maxAmount) {
        this.paymentRepository = paymentRepository;
        this.outboxService = outboxService;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.processedEventService = processedEventService;
        this.maxAmount = maxAmount;
    }

    // Called by the Kafka listener. The payment row, its event and the "already handled" record
    // are saved in ONE transaction.
    @Transactional
    public void payForBooking(SeatsReservedEvent event) {
        if (!processedEventService.markIfNew(event.getEventId())) {
            log.info("Skipping duplicate event {} (seats-reserved)", event.getEventId());
            return;   // charged already, so no second charge and no second event
        }

        Payment payment = pay(event.getBookingId(), event.getUserId(), event.getTotalAmount());
        String eventId = UUID.randomUUID().toString();

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            outboxService.save(KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC, payment.getBookingId(), eventId,
                    new PaymentCompletedEvent(eventId, payment.getBookingId(), payment.getId(), payment.getAmount()));
            return;
        }

        List<Long> seatIds = event.getSeats().stream().map(SeatDetail::getSeatId).toList();
        outboxService.save(KafkaTopicConfig.PAYMENT_FAILED_TOPIC, payment.getBookingId(), eventId,
                new PaymentFailedEvent(eventId, payment.getBookingId(), event.getShowId(),
                        seatIds, payment.getFailureReason()));
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
        return saved;
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

    // The payment and the idempotency key are saved in ONE transaction.
    // A duplicate key fails on the unique constraint, and the payment rolls back with it.
    @Transactional
    public Payment payOnce(Long bookingId, Long userId, BigDecimal amount, String idempotencyKey, String requestHash) {
        Payment payment = pay(bookingId, userId, amount);   // joins this transaction

        IdempotencyKey keyRow = new IdempotencyKey();
        keyRow.setUserId(userId);
        keyRow.setIdempotencyKey(idempotencyKey);
        keyRow.setRequestHash(requestHash);
        keyRow.setPaymentId(payment.getId());
        idempotencyKeyRepository.save(keyRow);
        return payment;
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findReplay(Long userId, String idempotencyKey, String requestHash) {
        return idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(row -> {
                    if (!row.getRequestHash().equals(requestHash)) {
                        throw new IdempotencyKeyReuseException(
                                "Idempotency-Key '" + idempotencyKey + "' was already used with a different request");
                    }
                    return paymentRepository.findById(row.getPaymentId()).orElseThrow();
                });
    }
}