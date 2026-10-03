package com.rahul.cinemaservice.listner;

import com.rahul.cinemaservice.dto.ReservedSeat;
import com.rahul.cinemaservice.dto.SeatReservationResponse;
import com.rahul.cinemaservice.event.BookingCreatedEvent;
import com.rahul.cinemaservice.event.CinemaEventPublisher;
import com.rahul.cinemaservice.event.SeatDetail;
import com.rahul.cinemaservice.event.SeatsReservedEvent;
import com.rahul.cinemaservice.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedListener {

    private final SeatReservationService seatReservationService;
    private final CinemaEventPublisher eventPublisher;

    @KafkaListener(topics = "booking-created")
    public void onBookingCreated(@Payload BookingCreatedEvent event) {
        log.info("Received {}", event);

        // The same locking and checking code as before. Only the caller changed: a Kafka message, not a controller.
        SeatReservationResponse reservation =
                seatReservationService.reserveSeats(event.getShowId(), event.getSeatIds());

        List<SeatDetail> seats = reservation.getSeats().stream()
                .map(s -> new SeatDetail(s.getSeatId(), s.getSeatNumber(), s.getPrice()))
                .toList();

        BigDecimal total = reservation.getSeats().stream()
                .map(ReservedSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        eventPublisher.publishSeatsReserved(new SeatsReservedEvent(
                UUID.randomUUID().toString(),
                event.getBookingId(),
                event.getUserId(),
                event.getShowId(),
                reservation.getMovieName(),
                reservation.getShowTime(),
                total,
                seats));
    }
}