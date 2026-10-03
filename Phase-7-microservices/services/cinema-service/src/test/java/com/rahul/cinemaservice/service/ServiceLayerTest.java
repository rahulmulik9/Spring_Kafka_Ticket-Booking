package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.dto.MovieSummaryResponse;
import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.entity.SeatStatus;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.exception.MovieNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Runs against the real cinemadb container. Each test rolls back, so no test data is left behind.
@SpringBootTest
@Transactional
@WithMockUser(roles = "ORGANIZER")
class ServiceLayerTest {

    @Autowired
    private MovieService movieService;
    @Autowired
    private ShowService showService;
    @Autowired
    private SeatService seatService;

    private Movie newMovie(String name) {
        Movie movie = new Movie();
        movie.setName(name);
        movie.setDescription("test movie");
        return movieService.createMovie(movie);
    }

    @Test
    void createdMovieCanBeRead() {
        Movie saved = newMovie("Test Movie");

        assertNotNull(saved.getId());
        assertEquals("Test Movie", movieService.getMovieDetails(saved.getId()).getName());
    }

    @Test
    void missingMovieThrowsNotFound() {
        assertThrows(MovieNotFoundException.class, () -> movieService.getMovieDetails(999999L));
    }

    @Test
    void newShowGetsFiftySeatsAllAvailable() {
        Movie movie = newMovie("Seat Test Movie");
        Show show = new Show();
        show.setShowTime(LocalDateTime.now().plusDays(5));

        Show saved = showService.createShow(movie.getId(), show);
        List<Seat> seats = seatService.getSeatsByShow(saved.getId());

        assertEquals(50, seats.size());
        assertTrue(seats.stream().allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE));
        assertTrue(seats.stream().anyMatch(s -> "A1".equals(s.getSeatNumber())));
        assertTrue(seats.stream().anyMatch(s -> "E10".equals(s.getSeatNumber())));
    }

    @Test
    void showForMissingMovieIsRejected() {
        assertThrows(MovieNotFoundException.class, () -> showService.createShow(999999L, new Show()));
    }

    @Test
    void sampleMovieHasTwoShows() {
        assertEquals(2, showService.getShowsByMovie(1L).size());
    }

    @Test
    void searchFindsMovieByExactName() {
        newMovie("Unique Search Name");

        Page<MovieSummaryResponse> page =
                movieService.searchMoviesByName("Unique Search Name", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }
}