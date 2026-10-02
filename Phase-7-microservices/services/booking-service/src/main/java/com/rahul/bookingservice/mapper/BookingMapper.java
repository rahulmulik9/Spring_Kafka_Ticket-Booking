package com.rahul.bookingservice.mapper;

import com.rahul.bookingservice.dto.BookingResponse;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingSeat;

import java.util.List;

public class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {
        List<String> seatNumbers = booking.getSeats().stream()
                .map(BookingSeat::getSeatNumber)
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getShowId(),
                booking.getMovieName(),
                booking.getShowTime(),
                booking.getCustomerEmail(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                seatNumbers,
                booking.getCreatedAt()
        );
    }
}