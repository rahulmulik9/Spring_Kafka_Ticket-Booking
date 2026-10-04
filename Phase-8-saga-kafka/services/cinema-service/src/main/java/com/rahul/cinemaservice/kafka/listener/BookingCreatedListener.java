package com.rahul.cinemaservice.kafka.listener;

import com.rahul.cinemaservice.dto.ReservedSeat;
import com.rahul.cinemaservice.dto.SeatReservationResponse;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.exception.SeatAlreadyBookedException;
import com.rahul.cinemaservice.exception.SeatDoesNotBelongToShowException;
import com.rahul.cinemaservice.exception.SeatNotFoundException;
import com.rahul.cinemaservice.exception.ShowNotFoundException;
import com.rahul.cinemaservice.kafka.event.BookingCreatedEvent;
import com.rahul.cinemaservice.kafka.event.SeatDetail;
import com.rahul.cinemaservice.kafka.event.SeatsReservationFailedEvent;
import com.rahul.cinemaservice.kafka.event.SeatsReservedEvent;
import com.rahul.cinemaservice.kafka.publisher.CinemaEventPublisher;
import com.rahul.cinemaservice.repository.SeatRepository;
import com.rahul.cinemaservice.repository.ShowRepository;
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
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    @KafkaListener(topics = "booking-created",
            properties = "spring.json.value.default.type=com.rahul.cinemaservice.kafka.event.BookingCreatedEvent")
    public void onBookingCreated(@Payload BookingCreatedEvent event) {
        log.info("Received {}", event);

        SeatReservationResponse reservation;
        try {
            reservation = seatReservationService.reserveSeats(event.getShowId(), event.getSeatIds());
        } catch (SeatAlreadyBookedException | SeatNotFoundException | SeatDoesNotBelongToShowException | ShowNotFoundException ex) {
            Show show = showRepository.findByIdWithMovie(event.getShowId()).orElse(null);
            String movieName = show != null ? show.getMovie().getName() : null;
            java.time.LocalDateTime showTime = show != null ? show.getShowTime() : null;

            // Plain read, no lock: we only want the numbers and prices for the message.
            List<SeatDetail> requested = seatRepository.findAllById(event.getSeatIds()).stream()
                    .map(s -> new SeatDetail(s.getId(), s.getSeatNumber(), s.getPrice()))
                    .toList();

            eventPublisher.publishSeatsReservationFailed(new SeatsReservationFailedEvent(
                    UUID.randomUUID().toString(), event.getBookingId(), ex.getMessage(),
                    movieName, showTime, requested));
            return;
        }

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