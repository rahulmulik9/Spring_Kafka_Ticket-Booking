package com.rahul.bookingservice.service;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.exception.BookingNotFoundException;
import com.rahul.bookingservice.kafka.config.KafkaTopicConfig;
import com.rahul.bookingservice.kafka.event.BookingCancelledEvent;
import com.rahul.bookingservice.kafka.event.BookingConfirmedEvent;
import com.rahul.bookingservice.kafka.event.BookingCreatedEvent;
import com.rahul.bookingservice.kafka.event.BookingFailedEvent;
import com.rahul.bookingservice.kafka.event.PaymentFailedEvent;
import com.rahul.bookingservice.kafka.event.SeatDetail;
import com.rahul.bookingservice.kafka.event.SeatsReservationFailedEvent;
import com.rahul.bookingservice.kafka.event.SeatsReservedEvent;
import com.rahul.bookingservice.outbox.OutboxService;
import com.rahul.bookingservice.repository.BookingRepository;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Only database work lives here. Every method is one short transaction.
// Each status change saves its event to the outbox in the SAME transaction.
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OutboxService outboxService;

    @Transactional
    public Booking createPendingBooking(Long showId, AuthUser user, List<Long> seatIds) {
        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setShowId(showId);
        booking.setCustomerEmail(user.getEmail());
        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);   // IDENTITY: the id exists after this line

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.BOOKING_CREATED_TOPIC, saved.getId(), eventId,
                new BookingCreatedEvent(eventId, saved.getId(), user.getId(), showId, seatIds));
        return saved;
    }

    // Cinema reserved the seats: copy the details into the booking. No event is needed.
    @Transactional
    public void addReservationDetails(SeatsReservedEvent event) {
        Booking booking = findWithSeats(event.getBookingId());

        booking.setMovieName(event.getMovieName());
        booking.setShowTime(event.getShowTime());
        booking.setTotalAmount(event.getTotalAmount());

        for (SeatDetail detail : event.getSeats()) {
            BookingSeat seat = new BookingSeat();
            seat.setSeatId(detail.getSeatId());
            seat.setSeatNumber(detail.getSeatNumber());
            seat.setPrice(detail.getPrice());
            booking.addSeat(seat);
        }
    }

    // Payment succeeded.
    @Transactional
    public void markConfirmed(Long bookingId) {
        Booking booking = findWithSeats(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.BOOKING_CONFIRMED_TOPIC, bookingId, eventId,
                new BookingConfirmedEvent(eventId, bookingId, booking.getCustomerEmail()));
    }

    // Payment was declined: tell Cinema (release seats) and Notification.
    @Transactional
    public void markPaymentFailed(PaymentFailedEvent event) {
        Booking booking = findWithSeats(event.getBookingId());
        booking.setStatus(BookingStatus.PAYMENT_FAILED);

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.BOOKING_FAILED_TOPIC, booking.getId(), eventId,
                new BookingFailedEvent(eventId, booking.getId(), event.getShowId(), event.getSeatIds(),
                        booking.getCustomerEmail(), event.getReason()));
    }

    // A seat was taken: keep what the customer tried to book. Nothing was reserved, so no seats to release.
    @Transactional
    public void markSeatsUnavailable(SeatsReservationFailedEvent event) {
        Booking booking = findWithSeats(event.getBookingId());

        booking.setStatus(BookingStatus.SEATS_UNAVAILABLE);
        booking.setMovieName(event.getMovieName());
        booking.setShowTime(event.getShowTime());

        BigDecimal total = BigDecimal.ZERO;
        if (event.getSeats() != null) {
            for (SeatDetail detail : event.getSeats()) {
                BookingSeat seat = new BookingSeat();
                seat.setSeatId(detail.getSeatId());
                seat.setSeatNumber(detail.getSeatNumber());
                seat.setPrice(detail.getPrice());
                booking.addSeat(seat);
                total = total.add(detail.getPrice());
            }
        }
        booking.setTotalAmount(total);

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.BOOKING_FAILED_TOPIC, booking.getId(), eventId,
                new BookingFailedEvent(eventId, booking.getId(), booking.getShowId(), List.of(),
                        booking.getCustomerEmail(), event.getReason()));
    }

    // The customer cancelled a confirmed booking.
    @Transactional
    public Booking markCancelled(Long bookingId) {
        Booking booking = findWithSeats(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);

        List<Long> seatIds = booking.getSeats().stream().map(BookingSeat::getSeatId).toList();

        String eventId = UUID.randomUUID().toString();
        outboxService.save(KafkaTopicConfig.BOOKING_CANCELLED_TOPIC, bookingId, eventId,
                new BookingCancelledEvent(eventId, bookingId, booking.getShowId(), seatIds,
                        booking.getCustomerEmail()));
        return booking;
    }

    // The owner or an admin may read a booking. Anyone else gets a 403.
    @Transactional(readOnly = true)
    public Booking getBookingForUser(Long id, AuthUser caller) {
        Booking booking = findWithSeats(id);

        boolean isOwner = booking.getUserId().equals(caller.getId());
        boolean isAdmin = "ADMIN".equals(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only access your own bookings");
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(AuthUser caller) {
        return bookingRepository.findAllByUserIdWithSeats(caller.getId());
    }

    private Booking findWithSeats(Long bookingId) {
        return bookingRepository.findByIdWithSeats(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
    }
}