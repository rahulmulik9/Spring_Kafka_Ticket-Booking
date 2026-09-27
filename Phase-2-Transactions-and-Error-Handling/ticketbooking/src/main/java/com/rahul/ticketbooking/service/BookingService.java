package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.*;
import com.rahul.ticketbooking.repository.BookingRepository;
import com.rahul.ticketbooking.repository.SeatRepository;
import com.rahul.ticketbooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final AuditService auditService;

    @Transactional
    public Booking createBooking(Long showId, BookingRequest request) {
        try {
            Show show = showRepository.findById(showId)
                    .orElseThrow(() -> new RuntimeException("Show not found with id: " + showId));

            List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

            if (seats.size() != request.getSeatIds().size()) {
                throw new RuntimeException("One or more seats do not exist");
            }

            for (Seat seat : seats) {
                if (!seat.getShow().getId().equals(showId)) {
                    throw new RuntimeException("Seat " + seat.getSeatNumber() + " does not belong to this show");
                }
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new RuntimeException("Seat " + seat.getSeatNumber() + " is already booked");
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
            booking.setCustomerName(request.getCustomerName());
            booking.setCustomerEmail(request.getCustomerEmail());
            booking.setTotalAmount(totalAmount);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setCreatedAt(LocalDateTime.now());
            booking.setSeats(seats);

            Booking saved = bookingRepository.save(booking);

            auditService.logAttempt(showId, request.getCustomerEmail(), true, null);

            return saved;

        } catch (RuntimeException ex) {
            auditService.logAttempt(showId, request.getCustomerEmail(), false, ex.getMessage());
            throw ex;
        }
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    @Transactional
    public Booking cancelBooking(Long id) {
        Booking booking = getBookingById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking " + id + " is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        for (Seat seat : booking.getSeats()) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }

        seatRepository.saveAll(booking.getSeats());
        return bookingRepository.save(booking);
    }
}