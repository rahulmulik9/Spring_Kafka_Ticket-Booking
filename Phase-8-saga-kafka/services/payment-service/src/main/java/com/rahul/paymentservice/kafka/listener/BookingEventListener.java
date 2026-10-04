package com.rahul.paymentservice.kafka.listener;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.PaymentStatus;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
import com.rahul.paymentservice.kafka.event.SeatDetail;
import com.rahul.paymentservice.kafka.publisher.PaymentEventPublisher;
import com.rahul.paymentservice.kafka.event.SeatsReservedEvent;
import com.rahul.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final PaymentService paymentService;
    private final PaymentEventPublisher eventPublisher;

    @KafkaListener(topics = "seats-reserved", groupId = "payment-service")
    public void onSeatsReserved(@Payload SeatsReservedEvent event) {
        log.info("Received {}", event);

        Payment payment = paymentService.pay(event.getBookingId(), event.getUserId(), event.getTotalAmount());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                    UUID.randomUUID().toString(),
                    payment.getBookingId(),
                    payment.getId(),
                    payment.getAmount()));
            return;
        }

        List<Long> seatIds = event.getSeats().stream().map(SeatDetail::getSeatId).toList();
        eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                payment.getBookingId(),
                event.getShowId(),
                seatIds,
                payment.getFailureReason()));
    }
}