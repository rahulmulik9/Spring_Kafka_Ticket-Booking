package com.rahul.ticketbooking.booking.service;

import com.rahul.ticketbooking.booking.dto.BookingRequest;
import com.rahul.ticketbooking.booking.repository.BookingRepository;
import com.rahul.ticketbooking.seat.repository.SeatRepository;
import com.rahul.ticketbooking.show.repository.ShowRepository;
import com.rahul.ticketbooking.user.repository.UserRepository;
import com.rahul.ticketbooking.auth.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import com.rahul.ticketbooking.auth.exception.InvalidCredentialsException;
import com.rahul.ticketbooking.booking.entity.Booking;
import com.rahul.ticketbooking.booking.entity.BookingStatus;
import com.rahul.ticketbooking.booking.exception.BookingAlreadyCancelledException;
import com.rahul.ticketbooking.booking.exception.BookingNotFoundException;
import com.rahul.ticketbooking.seat.entity.Seat;
import com.rahul.ticketbooking.seat.entity.SeatStatus;
import com.rahul.ticketbooking.seat.exception.SeatAlreadyBookedException;
import com.rahul.ticketbooking.seat.exception.SeatDoesNotBelongToShowException;
import com.rahul.ticketbooking.seat.exception.SeatNotFoundException;
import com.rahul.ticketbooking.show.entity.Show;
import com.rahul.ticketbooking.show.exception.ShowNotFoundException;
import com.rahul.ticketbooking.user.entity.User;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;

    // Step 5: pessimistic. SELECT ... FOR UPDATE, others wait for the row lock.
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public Booking createBooking(Long showId, BookingRequest request, Long userId) {
        return book(showId, request, userId, seatRepository::findAllByIdForUpdate);
    }

    // Step 7: optimistic, for comparison only. Relies on @Version, no row lock taken on read.
    @Transactional
    public Booking createBookingOptimistic(Long showId, BookingRequest request, Long userId) {
        return book(showId, request, userId, seatRepository::findAllById);
    }

    // Step 7: shared logic. Only how seats are fetched differs between the two callers above.
    private Booking book(Long showId, BookingRequest request, Long userId,
                         Function<List<Long>, List<Seat>> seatFetcher) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User no longer exists"));

        Show show = showRepository.findByIdWithMovie(showId)
                .orElseThrow(() -> new ShowNotFoundException("Show not found with id: " + showId));

        List<Seat> seats = seatFetcher.apply(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new SeatNotFoundException("One or more seats do not exist");
        }

        for (Seat seat : seats) {
            if (!seat.getShow().getId().equals(showId)) {
                throw new SeatDoesNotBelongToShowException(
                        "Seat " + seat.getSeatNumber() + " does not belong to this show");
            }
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatAlreadyBookedException(
                        "Seat " + seat.getSeatNumber() + " is already booked");
            }
        }

        BigDecimal totalAmount = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        seatRepository.saveAll(seats);

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setUser(user);
        booking.setCustomerName(user.getName());
        booking.setCustomerEmail(user.getEmail());
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setSeats(seats);

        return bookingRepository.save(booking);
    }

    // Internal lookup with NO ownership check. Only other service methods should call this.
    public Booking getBookingById(Long id) {
        return bookingRepository.findByIdWithShowAndMovie(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    // Used by the controller. Same lookup, plus the ownership check.
    // @Transactional so the lazy user can be read while the session is still open.
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Booking getBookingForUser(Long id, AuthUser caller) {
        Booking booking = getBookingById(id);
        checkOwnerOrAdmin(booking, caller);
        return booking;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public Booking cancelBooking(Long id, AuthUser caller) {
        Booking booking = getBookingById(id);
        checkOwnerOrAdmin(booking, caller);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking " + id + " is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        for (Seat seat : booking.getSeats()) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }

        seatRepository.saveAll(booking.getSeats());
        return bookingRepository.save(booking);
    }

    // The owner or an admin may proceed. Anyone else gets a 403 (AccessDeniedException).
    private void checkOwnerOrAdmin(Booking booking, AuthUser caller) {
        boolean isOwner = booking.getUser().getId().equals(caller.getId());
        boolean isAdmin = "ADMIN".equals(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only access your own bookings");
        }
    }
}