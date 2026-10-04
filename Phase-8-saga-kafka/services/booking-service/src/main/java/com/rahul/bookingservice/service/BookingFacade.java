package com.rahul.bookingservice.service;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.exception.BookingAlreadyCancelledException;
import com.rahul.bookingservice.exception.InvalidBookingStateException;
import com.rahul.bookingservice.kafka.event.BookingCancelledEvent;
import com.rahul.bookingservice.kafka.event.BookingCreatedEvent;
import com.rahul.bookingservice.kafka.publisher.BookingEventPublisher;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/*
 * Known gaps, fixed in the next steps:
 *  - Crash between saving and publishing loses the message (Steps 5 and 6, outbox).
 *  - The same request twice creates two bookings (Step 7, idempotency key).
 *  - Duplicate messages are not detected yet (Step 8).
 *  - Cancel does not refund, because no refund flow exists yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final AuditService auditService;
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
        Booking booking = bookingService.createPendingBooking(showId, user);

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

        Booking cancelled = bookingService.updateStatus(bookingId, BookingStatus.CANCELLED);

        // Cinema releases the seats and Notification tells the customer, both from this one event.
        eventPublisher.publishBookingCancelled(new BookingCancelledEvent(
                UUID.randomUUID().toString(),
                bookingId,
                booking.getShowId(),
                seatIdsOf(booking),
                booking.getCustomerEmail()));

        return cancelled;
    }

    private List<Long> seatIdsOf(Booking booking) {
        return booking.getSeats().stream().map(BookingSeat::getSeatId).toList();
    }
}