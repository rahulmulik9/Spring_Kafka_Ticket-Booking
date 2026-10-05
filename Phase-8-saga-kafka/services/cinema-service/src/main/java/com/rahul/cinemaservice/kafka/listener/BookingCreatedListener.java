package com.rahul.cinemaservice.kafka.listener;

import com.rahul.cinemaservice.exception.SeatAlreadyBookedException;
import com.rahul.cinemaservice.exception.SeatDoesNotBelongToShowException;
import com.rahul.cinemaservice.exception.SeatNotFoundException;
import com.rahul.cinemaservice.exception.ShowNotFoundException;
import com.rahul.cinemaservice.kafka.event.BookingCreatedEvent;
import com.rahul.cinemaservice.service.BookingReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedListener {

    private final BookingReservationService bookingReservationService;

    @KafkaListener(topics = "booking-created",
            properties = "spring.json.value.default.type=com.rahul.cinemaservice.kafka.event.BookingCreatedEvent")
    public void onBookingCreated(@Payload BookingCreatedEvent event) {
        log.info("Received {}", event);

        try {
            bookingReservationService.reserveAndRecord(event);
        } catch (SeatAlreadyBookedException | SeatNotFoundException
                 | SeatDoesNotBelongToShowException | ShowNotFoundException ex) {
            // A business refusal, not a crash. The first transaction rolled back, so nothing was changed.
            bookingReservationService.recordFailure(event, ex.getMessage());
        }
    }
}