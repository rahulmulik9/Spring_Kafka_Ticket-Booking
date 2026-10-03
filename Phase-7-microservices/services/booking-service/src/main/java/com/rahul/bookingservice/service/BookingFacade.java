package com.rahul.bookingservice.service;

import com.rahul.bookingservice.client.CinemaClient;
import com.rahul.bookingservice.client.NotificationClient;
import com.rahul.bookingservice.client.PaymentClient;
import com.rahul.bookingservice.client.dto.NotificationRequest;
import com.rahul.bookingservice.client.dto.PaymentRequest;
import com.rahul.bookingservice.client.dto.PaymentResult;
import com.rahul.bookingservice.client.dto.SeatIdsRequest;
import com.rahul.bookingservice.client.dto.SeatReservationResponse;
import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.exception.BookingAlreadyCancelledException;
import com.rahul.bookingservice.exception.InvalidBookingStateException;
import com.rahul.bookingservice.exception.PaymentFailedException;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * Orchestrates the booking across services. NOT @Transactional on purpose:
 * a database transaction must never stay open while we wait for another service.
 *
 * Known gaps, fixed in later phases:
 *  - If this service crashes between two steps, seats can stay BOOKED with no booking (Phase 8, saga).
 *  - If payment charges but its reply is lost, we release the seats while the money is taken (Phase 8, idempotency).
 *  - Cancelling does not refund, because payment-service has no refund call yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final AuditService auditService;
    private final CinemaClient cinemaClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;

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
        SeatIdsRequest seatIds = new SeatIdsRequest(request.getSeatIds());

        // 1. Reserve the seats in cinema-service (it locks and checks them)
        SeatReservationResponse reservation = cinemaClient.reserveSeats(showId, seatIds);

        // 2. Save the booking as PENDING. Payment needs a booking number to charge against.
        Booking booking;
        try {
            booking = bookingService.createPendingBooking(showId, user, reservation);
        } catch (RuntimeException ex) {
            releaseSeatsQuietly(showId, seatIds);
            throw ex;
        }

        // 3. Charge the customer
        PaymentResult payment;
        try {
            payment = paymentClient.pay(new PaymentRequest(booking.getId(), booking.getTotalAmount()));
        } catch (RuntimeException ex) {
            failBooking(booking, showId, seatIds);   // payment-service was unreachable or refused the call
            throw ex;
        }

        if (!"SUCCESS".equals(payment.getStatus())) {
            failBooking(booking, showId, seatIds);
            throw new PaymentFailedException(booking.getId(), payment.getFailureReason());
        }

        // 4. Paid: confirm the booking
        Booking confirmed = bookingService.updateStatus(booking.getId(), BookingStatus.CONFIRMED);
        notifyQuietly(user.getEmail(), "BOOKING_CONFIRMED", confirmed.getId());
        return confirmed;
    }

    // Payment did not go through: give the seats back and keep a record of the failed booking.
    private void failBooking(Booking booking, Long showId, SeatIdsRequest seatIds) {
        releaseSeatsQuietly(showId, seatIds);
        bookingService.updateStatus(booking.getId(), BookingStatus.PAYMENT_FAILED);
        notifyQuietly(booking.getCustomerEmail(), "PAYMENT_FAILED", booking.getId());
    }

    private void releaseSeatsQuietly(Long showId, SeatIdsRequest seatIds) {
        try {
            cinemaClient.releaseSeats(showId, seatIds);
        } catch (RuntimeException ex) {
            log.error("Could not release seats {} of show {}. They stay BOOKED until fixed by hand. Reason: {}",
                    seatIds.getSeatIds(), showId, ex.getMessage());
        }
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

        // Release first, then mark cancelled. Release is safe to repeat, so if saving the
        // status fails, the customer can simply press cancel again.
        cinemaClient.releaseSeats(booking.getShowId(), new SeatIdsRequest(seatIdsOf(booking)));
        Booking cancelled = bookingService.updateStatus(bookingId, BookingStatus.CANCELLED);

        notifyQuietly(booking.getCustomerEmail(), "BOOKING_CANCELLED", bookingId);
        return cancelled;
    }

    // A message that fails to send must never undo a booking.
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