package com.rahul.bookingservice.service;

import com.rahul.bookingservice.client.CinemaClient;
import com.rahul.bookingservice.client.NotificationClient;
import com.rahul.bookingservice.client.dto.NotificationRequest;
import com.rahul.bookingservice.client.dto.SeatIdsRequest;
import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.event.BookingCreatedEvent;
import com.rahul.bookingservice.event.BookingEventPublisher;
import com.rahul.bookingservice.exception.BookingAlreadyCancelledException;
import com.rahul.bookingservice.exception.InvalidBookingStateException;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/*
 * Step 3: the facade only saves a PENDING booking and publishes BookingCreated.
 * Cinema, Payment and the listeners in this service do the rest.
 *
 * Known gaps, fixed in the next steps:
 *  - Seat taken or payment declined: the booking stays PENDING (Step 4, saga).
 *  - Crash between saving and publishing loses the message (Steps 5 and 6, outbox).
 *  - The same request twice creates two bookings (Step 7, idempotency key).
 *  - Cancel still uses Feign calls (Step 4 turns it into an event).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final AuditService auditService;
    private final CinemaClient cinemaClient;
    private final NotificationClient notificationClient;
    private final BookingEventPublisher eventPublisher;

    public Booking createBooking(Long showId, BookingRequest request, AuthUser user) {
        try {
            Booking booking = doCreateBooking(showId, request, user);
            auditService.logAttempt(showId, user.getEmail(), true, null);
            return booking;
        } catch (RuntimeException ex) {
            auditService.logAttempt(showId, user.getEmail(), false, ex.getMessage());
            throw ex;
        }
    }

    private Booking doCreateBooking(Long showId, BookingRequest request, AuthUser user) {
        // 1. Save the booking as PENDING (its own short transaction, committed when this returns)
        Booking booking = bookingService.createPendingBooking(showId, user);

        // 2. Tell the world. Cinema will pick it up and reserve the seats.
        eventPublisher.publishBookingCreated(new BookingCreatedEvent(
                UUID.randomUUID().toString(),
                booking.getId(),
                user.getId(),
                showId,
                request.getSeatIds()));

        return booking;
    }

    public Booking cancelBooking(Long bookingId, AuthUser user) {
        Booking booking = bookingService.getBookingForUser(bookingId, user);   // 404 or 403 here

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking " + bookingId + " is already cancelled");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Only confirmed bookings can be cancelled. This booking is " + booking.getStatus());
        }

        cinemaClient.releaseSeats(booking.getShowId(), new SeatIdsRequest(seatIdsOf(booking)));
        Booking cancelled = bookingService.updateStatus(bookingId, BookingStatus.CANCELLED);

        notifyQuietly(booking.getCustomerEmail(), "BOOKING_CANCELLED", bookingId);
        return cancelled;
    }

    private void notifyQuietly(String email, String type, Long bookingId) {
        try {
            notificationClient.send(new NotificationRequest(email, type, bookingId));
        } catch (RuntimeException ex) {
            log.warn("Notification {} for booking {} was not sent: {}", type, bookingId, ex.getMessage());
        }
    }

    private List<Long> seatIdsOf(Booking booking) {
        return booking.getSeats().stream().map(BookingSeat::getSeatId).toList();
    }
}