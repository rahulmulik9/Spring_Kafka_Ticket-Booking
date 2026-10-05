package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.dto.SeatReservationResponse;
import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.entity.SeatStatus;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.exception.SeatAlreadyBookedException;
import com.rahul.cinemaservice.exception.SeatDoesNotBelongToShowException;
import com.rahul.cinemaservice.exception.SeatNotFoundException;
import com.rahul.cinemaservice.exception.ShowNotFoundException;
import com.rahul.cinemaservice.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Each test rolls back, so no test data is left in cinemadb.
@SpringBootTest
@Transactional
@WithMockUser(roles = "ORGANIZER")
class SeatReservationServiceTest {

    @Autowired
    private MovieService movieService;
    @Autowired
    private ShowService showService;
    @Autowired
    private SeatReservationService reservationService;
    @Autowired
    private SeatRepository seatRepository;

    private Show show;
    private List<Seat> seats;

    @BeforeEach
    void setUp() {
        Movie movie = new Movie();
        movie.setName("Reservation Test Movie");
        movie = movieService.createMovie(movie);

        Show newShow = new Show();
        newShow.setShowTime(LocalDateTime.now().plusDays(5));
        show = showService.createShow(movie.getId(), newShow);
        seats = seatRepository.findByShowId(show.getId());
    }

    private Long seatId(int index) {
        return seats.get(index).getId();
    }

    private SeatStatus statusOf(Long seatId) {
        return seatRepository.findById(seatId).orElseThrow().getStatus();
    }

    @Test
    void reserveMarksSeatsBookedAndReturnsDetails() {
        SeatReservationResponse response = reservationService.reserveSeats(show.getId(), List.of(seatId(0), seatId(1)));

        assertEquals("Reservation Test Movie", response.getMovieName());
        assertEquals(2, response.getSeats().size());
        assertEquals(0, response.getSeats().get(0).getPrice().compareTo(java.math.BigDecimal.valueOf(250)));
        assertEquals(SeatStatus.BOOKED, statusOf(seatId(0)));
        assertEquals(SeatStatus.BOOKED, statusOf(seatId(1)));
    }

    @Test
    void reservingTheSameSeatTwiceIsRejected() {
        reservationService.reserveSeats(show.getId(), List.of(seatId(0)));

        assertThrows(SeatAlreadyBookedException.class,
                () -> reservationService.reserveSeats(show.getId(), List.of(seatId(0))));
    }

    @Test
    void oneTakenSeatMeansNoSeatIsChanged() {
        reservationService.reserveSeats(show.getId(), List.of(seatId(0)));

        assertThrows(SeatAlreadyBookedException.class,
                () -> reservationService.reserveSeats(show.getId(), List.of(seatId(1), seatId(0))));

        assertEquals(SeatStatus.AVAILABLE, statusOf(seatId(1)));
    }

    @Test
    void duplicateIdsInOneRequestCountAsOneSeat() {
        SeatReservationResponse response =
                reservationService.reserveSeats(show.getId(), List.of(seatId(0), seatId(0)));

        assertEquals(1, response.getSeats().size());
    }

    @Test
    void seatFromAnotherShowIsRejected() {
        assertThrows(SeatDoesNotBelongToShowException.class,
                () -> reservationService.reserveSeats(show.getId(), List.of(1L)));   // seat 1 belongs to the sample show 1
    }

    @Test
    void missingSeatIsRejected() {
        assertThrows(SeatNotFoundException.class,
                () -> reservationService.reserveSeats(show.getId(), List.of(999999L)));
    }

    @Test
    void missingShowIsRejected() {
        assertThrows(ShowNotFoundException.class,
                () -> reservationService.reserveSeats(999999L, List.of(seatId(0))));
    }

    @Test
    void releaseMakesSeatsAvailableAndCanRepeat() {
        reservationService.reserveSeats(show.getId(), List.of(seatId(0)));

        reservationService.releaseSeats(show.getId(), List.of(seatId(0)));
        reservationService.releaseSeats(show.getId(), List.of(seatId(0)));   // second call must not fail

        assertEquals(SeatStatus.AVAILABLE, statusOf(seatId(0)));
    }
}