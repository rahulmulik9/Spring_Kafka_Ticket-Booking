package com.rahul.cinemaservice.kafka.listener;

import com.rahul.cinemaservice.kafka.event.BookingCancelledEvent;
import com.rahul.cinemaservice.kafka.event.BookingFailedEvent;
import com.rahul.cinemaservice.service.BookingReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// The compensating actions of the saga: give the seats back.
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReleaseListener {

    private final BookingReservationService bookingReservationService;

    @KafkaListener(topics = "booking-failed",
            properties = "spring.json.value.default.type=com.rahul.cinemaservice.kafka.event.BookingFailedEvent")
    public void onBookingFailed(@Payload BookingFailedEvent event) {
        log.info("Received {}", event);
        bookingReservationService.releaseAndRecord(event.getEventId(), event.getShowId(), event.getSeatIds());
    }

    @KafkaListener(topics = "booking-cancelled",
            properties = "spring.json.value.default.type=com.rahul.cinemaservice.kafka.event.BookingCancelledEvent")
    public void onBookingCancelled(@Payload BookingCancelledEvent event) {
        log.info("Received {}", event);
        bookingReservationService.releaseAndRecord(event.getEventId(), event.getShowId(), event.getSeatIds());
    }
}