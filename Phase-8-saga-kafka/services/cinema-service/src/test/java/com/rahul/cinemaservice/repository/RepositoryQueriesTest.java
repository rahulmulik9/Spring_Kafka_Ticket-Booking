package com.rahul.cinemaservice.repository;

import com.rahul.cinemaservice.dto.MovieSummaryResponse;
import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.entity.Show;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Runs against the real cinemadb container, so Docker must be up.
@SpringBootTest
@Transactional
class RepositoryQueriesTest {

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private SeatRepository seatRepository;

    @Test
    void searchByNameFindsSampleMovie() {
        Page<MovieSummaryResponse> page = movieRepository.searchByName("Inception", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Inception", page.getContent().get(0).getName());
    }

    @Test
    void summaryPageReturnsOnlyOnePage() {
        Page<MovieSummaryResponse> page = movieRepository.findSummaryPage(PageRequest.of(0, 1));

        assertEquals(1, page.getContent().size());
        assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    void showsOfFirstMovie() {
        assertEquals(2, showRepository.findByMovieId(1L).size());
    }

    @Test
    void showLoadsTogetherWithItsMovie() {
        Show show = showRepository.findByIdWithMovie(1L).orElseThrow();

        assertEquals("Inception", show.getMovie().getName());
    }

    @Test
    void firstShowHasFiftySeats() {
        assertEquals(50, seatRepository.findByShowId(1L).size());
    }

    @Test
    void lockQueryReturnsSeatsInIdOrder() {
        List<Seat> seats = seatRepository.findAllByIdForUpdate(List.of(3L, 1L, 2L));

        assertEquals(List.of(1L, 2L, 3L), seats.stream().map(Seat::getId).toList());
    }
}