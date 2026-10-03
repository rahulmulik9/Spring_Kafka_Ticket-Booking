package com.rahul.notificationservice.kafka.listener;

import com.rahul.notificationservice.kafka.event.BookingConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingConfirmedListener {

    @KafkaListener(topics = "booking-confirmed")
    public void onBookingConfirmed(@Payload BookingConfirmedEvent event) {
        log.info("Sending confirmation to {} for booking {}", event.getCustomerEmail(), event.getBookingId());
    }
}