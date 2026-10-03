package com.rahul.paymentservice.Kafka.listener;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.Kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.Kafka.publisher.PaymentEventPublisher;
import com.rahul.paymentservice.Kafka.event.SeatsReservedEvent;
import com.rahul.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final PaymentService paymentService;
    private final PaymentEventPublisher eventPublisher;

    @KafkaListener(topics = "seats-reserved")
    public void onSeatsReserved(@Payload SeatsReservedEvent event) {
        log.info("Received {}", event);

        Payment payment = paymentService.pay(event.getBookingId(), event.getUserId(), event.getTotalAmount());

        eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                payment.getBookingId(),
                payment.getId(),
                payment.getAmount()));
    }
}