package com.rahul.ticketbooking.booking.mapper;

import com.rahul.ticketbooking.booking.dto.BookingResponse;
import com.rahul.ticketbooking.booking.entity.Booking;
import com.rahul.ticketbooking.seat.entity.Seat;

import java.util.List;
import java.util.stream.Collectors;

public class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {
        List<String> seatNumbers = booking.getSeats().stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());

        return new BookingResponse(
                booking.getId(),
                booking.getShow().getId(),
                booking.getShow().getMovie().getName(),
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                seatNumbers,
                booking.getCreatedAt()
        );
    }
}