package com.rahul.bookingservice.service;

import com.rahul.bookingservice.client.dto.ReservedSeat;
import com.rahul.bookingservice.client.dto.SeatReservationResponse;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.exception.BookingNotFoundException;
import com.rahul.bookingservice.repository.BookingRepository;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Only database work lives here. Every method is one short transaction.
// Calls to other services happen in BookingFacade, outside any transaction.
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    @Transactional
    public Booking createPendingBooking(Long showId, AuthUser user, SeatReservationResponse reservation) {
        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setShowId(showId);
        booking.setMovieName(reservation.getMovieName());
        booking.setShowTime(reservation.getShowTime());
        booking.setCustomerEmail(user.getEmail());
        booking.setStatus(BookingStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (ReservedSeat reserved : reservation.getSeats()) {
            BookingSeat seat = new BookingSeat();
            seat.setSeatId(reserved.getSeatId());
            seat.setSeatNumber(reserved.getSeatNumber());
            seat.setPrice(reserved.getPrice());
            booking.addSeat(seat);
            total = total.add(reserved.getPrice());
        }
        booking.setTotalAmount(total);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findByIdWithSeats(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        booking.setStatus(status);
        return booking;
    }

    // The owner or an admin may read a booking. Anyone else gets a 403.
    @Transactional(readOnly = true)
    public Booking getBookingForUser(Long id, AuthUser caller) {
        Booking booking = bookingRepository.findByIdWithSeats(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        boolean isOwner = booking.getUserId().equals(caller.getId());
        boolean isAdmin = "ADMIN".equals(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only access your own bookings");
        }
        return booking;
    }
}