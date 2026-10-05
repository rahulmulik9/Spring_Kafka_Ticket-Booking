package com.rahul.bookingservice.kafka.listener;

import com.rahul.bookingservice.kafka.event.SeatsReservationFailedEvent;
import com.rahul.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatsReservationFailedListener {

    private final BookingService bookingService;

    @KafkaListener(topics = "seats-reservation-failed",
            properties = "spring.json.value.default.type=com.rahul.bookingservice.kafka.event.SeatsReservationFailedEvent")
    public void onSeatsReservationFailed(@Payload SeatsReservationFailedEvent event) {
        log.info("Received {}", event);
        bookingService.markSeatsUnavailable(event);
    }
}