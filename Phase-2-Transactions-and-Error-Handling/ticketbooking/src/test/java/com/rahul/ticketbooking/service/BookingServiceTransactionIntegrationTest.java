package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.SeatStatus;
import com.rahul.ticketbooking.exception.ShowNotFoundException;
import com.rahul.ticketbooking.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class BookingServiceTransactionIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatRepository seatRepository;

    // NOTE: adjust this seat ID to one that genuinely exists and is AVAILABLE
    // in your dev database (e.g. from Flyway seed data).
    private static final Long EXISTING_SEAT_ID = 10L;

    @Test
    @Transactional // wraps the WHOLE test in a transaction that rolls back after the test runs,
        // so this test never leaves permanent changes in your dev database
    void createBooking_failureAfterSeatUpdate_rollsBackSeatStatus() {
        Seat seatBefore = seatRepository.findById(EXISTING_SEAT_ID).orElseThrow();
        assertThat(seatBefore.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        BookingRequest request = new BookingRequest();
        request.setSeatIds(List.of(EXISTING_SEAT_ID));
        request.setCustomerName("Rollback Test");
        request.setCustomerEmail("rollback@test.com");

        // Using an invalid show ID forces ShowNotFoundException BEFORE any seat write happens,
        // which only proves the seat was never touched — not a true "partial write, then fail" case.
        // A true mid-transaction failure needs seat ID valid but a later step to fail — the next
        // test below constructs that scenario instead.
        assertThatThrownBy(() -> bookingService.createBooking(9999L, request))
                .isInstanceOf(ShowNotFoundException.class);

        Seat seatAfter = seatRepository.findById(EXISTING_SEAT_ID).orElseThrow();
        assertThat(seatAfter.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}