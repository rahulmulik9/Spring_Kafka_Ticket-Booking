package com.rahul.bookingservice.Kafka.listener;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.Kafka.event.BookingConfirmedEvent;
import com.rahul.bookingservice.Kafka.publisher.BookingEventPublisher;
import com.rahul.bookingservice.Kafka.event.PaymentCompletedEvent;
import com.rahul.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @KafkaListener(topics = "payment-completed", properties = "spring.json.value.default.type=com.rahul.bookingservice.Kafka.event.PaymentCompletedEvent")
    public void onPaymentCompleted(@Payload PaymentCompletedEvent event) {
        log.info("Received {}", event);

        // Own short transaction inside BookingService. No JWT is needed, so we call updateStatus directly.
        Booking confirmed = bookingService.updateStatus(event.getBookingId(), BookingStatus.CONFIRMED);

        eventPublisher.publishBookingConfirmed(new BookingConfirmedEvent(
                UUID.randomUUID().toString(),
                confirmed.getId(),
                confirmed.getCustomerEmail()));
    }
}