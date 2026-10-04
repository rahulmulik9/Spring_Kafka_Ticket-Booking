package com.rahul.bookingservice.kafka.listener;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.kafka.event.BookingFailedEvent;
import com.rahul.bookingservice.kafka.event.SeatsReservationFailedEvent;
import com.rahul.bookingservice.kafka.publisher.BookingEventPublisher;
import com.rahul.bookingservice.service.BookingService;
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
public class SeatsReservationFailedListener {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @KafkaListener(topics = "seats-reservation-failed",
            properties = "spring.json.value.default.type=com.rahul.bookingservice.kafka.event.SeatsReservationFailedEvent")
    public void onSeatsReservationFailed(@Payload SeatsReservationFailedEvent event) {
        log.info("Received {}", event);

        Booking booking = bookingService.markSeatsUnavailable(event);

        eventPublisher.publishBookingFailed(new BookingFailedEvent(
                UUID.randomUUID().toString(),
                booking.getId(),
                booking.getShowId(),
                List.of(),
                booking.getCustomerEmail(),
                event.getReason()));
    }
}