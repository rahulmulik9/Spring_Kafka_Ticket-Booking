package com.rahul.notificationservice.kafka.listener;

import com.rahul.notificationservice.idempotency.EventDeduplicator;
import com.rahul.notificationservice.kafka.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmedListener {

    private final EventDeduplicator deduplicator;

    @KafkaListener(topics = "booking-confirmed",
            properties = "spring.json.value.default.type=com.rahul.notificationservice.kafka.event.BookingConfirmedEvent")
    public void onBookingConfirmed(@Payload BookingConfirmedEvent event) {
        if (!deduplicator.firstTime(event.getEventId())) {
            log.info("Skipping duplicate event {} (booking-confirmed)", event.getEventId());
            return;
        }
        log.info("Sending confirmation to {} for booking {}", event.getCustomerEmail(), event.getBookingId());
    }
}