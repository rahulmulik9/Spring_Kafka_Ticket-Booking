package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.*;
import com.rahul.ticketbooking.repository.BookingRepository;
import com.rahul.ticketbooking.repository.SeatRepository;
import com.rahul.ticketbooking.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowRepository showRepository;

    @InjectMocks
    private BookingService bookingService;

    private Show show;
    private Seat seatA1;
    private Seat seatA2;

    @BeforeEach
    void setUp() {
        show = new Show();
        show.setId(1L);

        seatA1 = new Seat();
        seatA1.setId(1L);
        seatA1.setShow(show);
        seatA1.setSeatNumber("A1");
        seatA1.setStatus(SeatStatus.AVAILABLE);
        seatA1.setPrice(BigDecimal.valueOf(250));

        seatA2 = new Seat();
        seatA2.setId(2L);
        seatA2.setShow(show);
        seatA2.setSeatNumber("A2");
        seatA2.setStatus(SeatStatus.AVAILABLE);
        seatA2.setPrice(BigDecimal.valueOf(250));
    }

    @Test
    void createBooking_marksSeatsBookedAndCalculatesTotal() {
        BookingRequest request = new BookingRequest();
        request.setSeatIds(List.of(1L, 2L));
        request.setCustomerName("Rahul");
        request.setCustomerEmail("rahul@example.com");

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(seatA1, seatA2));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.createBooking(1L, request);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(seatA1.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seatA2.getStatus()).isEqualTo(SeatStatus.BOOKED);
        verify(seatRepository).saveAll(anyList());
    }

    @Test
    void createBooking_throwsWhenSeatAlreadyBooked() {
        seatA1.setStatus(SeatStatus.BOOKED);

        BookingRequest request = new BookingRequest();
        request.setSeatIds(List.of(1L));

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findAllById(List.of(1L))).thenReturn(List.of(seatA1));

        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already booked");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_throwsWhenSeatBelongsToDifferentShow() {
        Show otherShow = new Show();
        otherShow.setId(2L);
        seatA1.setShow(otherShow);

        BookingRequest request = new BookingRequest();
        request.setSeatIds(List.of(1L));

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findAllById(List.of(1L))).thenReturn(List.of(seatA1));

        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to this show");
    }

    @Test
    void cancelBooking_releasesSeatsAndMarksCancelled() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.CONFIRMED);
        seatA1.setStatus(SeatStatus.BOOKED);
        booking.setSeats(List.of(seatA1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(seatA1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void cancelBooking_throwsWhenAlreadyCancelled() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already cancelled");
    }
}