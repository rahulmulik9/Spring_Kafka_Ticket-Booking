package com.rahul.notificationservice.kafka.listener;

import com.rahul.notificationservice.kafka.event.BookingCancelledEvent;
import com.rahul.notificationservice.kafka.event.BookingFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingFailedListener {

    @KafkaListener(topics = "booking-failed",
            properties = "spring.json.value.default.type=com.rahul.notificationservice.kafka.event.BookingFailedEvent")
    public void onBookingFailed(@Payload BookingFailedEvent event) {
        log.info("Sending failure message to {} for booking {}: {}",
                event.getCustomerEmail(), event.getBookingId(), event.getReason());
    }

    @KafkaListener(topics = "booking-cancelled",
            properties = "spring.json.value.default.type=com.rahul.notificationservice.kafka.event.BookingCancelledEvent")
    public void onBookingCancelled(@Payload BookingCancelledEvent event) {
        log.info("Sending cancel message to {} for booking {}", event.getCustomerEmail(), event.getBookingId());
    }
}