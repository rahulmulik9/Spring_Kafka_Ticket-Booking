//package com.rahul.ticketbooking.service;
//
//import com.rahul.ticketbooking.dto.BookingRequest;
//import com.rahul.ticketbooking.entity.*;
//import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
//import com.rahul.ticketbooking.exception.ShowNotFoundException;
//import com.rahul.ticketbooking.repository.BookingRepository;
//import com.rahul.ticketbooking.repository.SeatRepository;
//import com.rahul.ticketbooking.repository.ShowRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class BookingServiceUnitTest {
//
//    @Mock
//    private BookingRepository bookingRepository;
//    @Mock
//    private SeatRepository seatRepository;
//    @Mock
//    private ShowRepository showRepository;
//    @Mock
//    private AuditService auditService;
//
//    @InjectMocks
//    private BookingService bookingService;
//
//    private Show show;
//    private Seat seat;
//    private BookingRequest request;
//
//    @BeforeEach
//    void setUp() {
//        Movie movie = new Movie();
//        movie.setName("Interstellar");
//
//        show = new Show();
//        show.setId(1L);
//        show.setMovie(movie);
//
//        seat = new Seat();
//        seat.setId(10L);
//        seat.setSeatNumber("A1");
//        seat.setStatus(SeatStatus.AVAILABLE);
//        seat.setShow(show);
//        seat.setPrice(BigDecimal.valueOf(250));
//
//        request = new BookingRequest();
//        request.setSeatIds(List.of(10L));
//        request.setCustomerName("Test User");
//        request.setCustomerEmail("test@test.com");
//    }
//
//    @Test
//    void createBooking_success_savesBookingAndLogsSuccess() {
//        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
//        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
//        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
//
//        Booking result = bookingService.createBooking(1L, request);
//
//        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
//        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
//        verify(seatRepository).saveAll(List.of(seat));
//        verify(auditService).logAttempt(1L, "test@test.com", true, null);
//    }
//
//    @Test
//    void createBooking_showNotFound_throwsAndLogsFailure() {
//        when(showRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
//                .isInstanceOf(ShowNotFoundException.class)
//                .hasMessageContaining("Show not found");
//
//        verify(auditService).logAttempt(eq(1L), eq("test@test.com"), eq(false), anyString());
//        verify(bookingRepository, never()).save(any());
//    }
//
//    @Test
//    void createBooking_seatAlreadyBooked_throwsAndLogsFailure() {
//        seat.setStatus(SeatStatus.BOOKED);
//        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
//        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
//
//        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
//                .isInstanceOf(SeatAlreadyBookedException.class);
//
//        verify(auditService).logAttempt(eq(1L), eq("test@test.com"), eq(false), anyString());
//        verify(bookingRepository, never()).save(any());
//    }
//}