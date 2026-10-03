package com.rahul.bookingservice.service;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.Kafka.event.SeatDetail;
import com.rahul.bookingservice.Kafka.event.SeatsReservedEvent;
import com.rahul.bookingservice.exception.BookingNotFoundException;
import com.rahul.bookingservice.repository.BookingRepository;
import com.rahul.bookingservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Only database work lives here. Every method is one short transaction.
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    // A bare booking: we do not know the movie, seats or price yet. Cinema tells us later.
    @Transactional
    public Booking createPendingBooking(Long showId, AuthUser user) {
        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setShowId(showId);
        booking.setCustomerEmail(user.getEmail());
        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    // Called when Cinema says the seats are reserved: copy the details into the booking.
    @Transactional
    public void addReservationDetails(SeatsReservedEvent event) {
        Booking booking = bookingRepository.findByIdWithSeats(event.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + event.getBookingId()));

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
        // No save() needed: the booking is managed, so dirty checking writes the changes at commit.
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

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(AuthUser caller) {
        return bookingRepository.findAllByUserIdWithSeats(caller.getId());
    }
}