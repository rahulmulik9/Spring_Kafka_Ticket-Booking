package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.dto.ReservedSeat;
import com.rahul.cinemaservice.dto.SeatReservationResponse;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.idempotency.ProcessedEventService;
import com.rahul.cinemaservice.kafka.config.KafkaTopicConfig;
import com.rahul.cinemaservice.kafka.event.BookingCreatedEvent;
import com.rahul.cinemaservice.kafka.event.SeatDetail;
import com.rahul.cinemaservice.kafka.event.SeatsReservationFailedEvent;
import com.rahul.cinemaservice.kafka.event.SeatsReservedEvent;
import com.rahul.cinemaservice.outbox.OutboxService;
import com.rahul.cinemaservice.repository.SeatRepository;
import com.rahul.cinemaservice.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingReservationService {

    private final SeatReservationService seatReservationService;
    private final OutboxService outboxService;
    private final ProcessedEventService processedEventService;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    // Seats become BOOKED, the SeatsReserved event is saved, and the event id is recorded, in ONE transaction.
    // If reserveSeats throws, all three roll back, including the record, and the listener calls recordFailure.
    @Transactional
    public void reserveAndRecord(BookingCreatedEvent event) {
        if (!processedEventService.markIfNew(event.getEventId())) {
            log.info("Skipping duplicate event {} (booking-created)", event.getEventId());
            return;
        }

        SeatReservationResponse reservation =
                seatReservationService.reserveSeats(event.getShowId(), event.getSeatIds());

        List<SeatDetail> seats = reservation.getSeats().stream()
                .map(s -> new SeatDetail(s.getSeatId(), s.getSeatNumber(), s.getPrice()))
                .toList();

        BigDecimal total = reservation.getSeats().stream()
                .map(ReservedSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.SEATS_RESERVED_TOPIC, event.getBookingId(), eventId,
                new SeatsReservedEvent(eventId, event.getBookingId(), event.getUserId(), event.getShowId(),
                        reservation.getMovieName(), reservation.getShowTime(), total, seats));
    }

    // Runs in a NEW transaction after reserveAndRecord rolled back: save the "no" answer as an event.
    // The rollback also removed the record from reserveAndRecord, so this method records the event id itself.
    @Transactional
    public void recordFailure(BookingCreatedEvent event, String reason) {
        if (!processedEventService.markIfNew(event.getEventId())) {
            log.info("Skipping duplicate event {} (booking-created, failure path)", event.getEventId());
            return;
        }

        Show show = showRepository.findByIdWithMovie(event.getShowId()).orElse(null);
        String movieName = show != null ? show.getMovie().getName() : null;
        LocalDateTime showTime = show != null ? show.getShowTime() : null;

        List<SeatDetail> requested = seatRepository.findAllById(event.getSeatIds()).stream()
                .map(s -> new SeatDetail(s.getId(), s.getSeatNumber(), s.getPrice()))
                .toList();

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.SEATS_RESERVATION_FAILED_TOPIC, event.getBookingId(), eventId,
                new SeatsReservationFailedEvent(eventId, event.getBookingId(), reason,
                        movieName, showTime, requested));
    }

    // The compensating action (booking-failed or booking-cancelled). Guarded, because a late duplicate
    // could otherwise release seats that someone else has booked since.
    @Transactional
    public void releaseAndRecord(String eventId, Long showId, List<Long> seatIds) {
        if (!processedEventService.markIfNew(eventId)) {
            log.info("Skipping duplicate event {} (seat release)", eventId);
            return;
        }
        if (seatIds == null || seatIds.isEmpty()) {
            return;   // seats were never reserved, nothing to give back
        }
        seatReservationService.releaseSeats(showId, seatIds);
    }
}