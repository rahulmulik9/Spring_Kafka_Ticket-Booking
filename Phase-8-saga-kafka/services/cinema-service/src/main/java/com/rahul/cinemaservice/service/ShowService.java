package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.entity.SeatStatus;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.repository.SeatRepository;
import com.rahul.cinemaservice.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private static final int ROWS = 5;
    private static final int SEATS_PER_ROW = 10;
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(250);

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final MovieService movieService;

    // One transaction: the show and its 50 seats are saved together or not at all.
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Transactional
    public Show createShow(Long movieId, Show show) {
        Movie movie = movieService.getMovieById(movieId);
        show.setMovie(movie);

        Show savedShow = showRepository.save(show);
        generateSeats(savedShow);

        return savedShow;
    }

    public List<Show> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId);
    }

    private void generateSeats(Show show) {
        List<Seat> seats = new ArrayList<>();
        char[] rowLetters = "ABCDE".toCharArray();

        for (int row = 0; row < ROWS; row++) {
            for (int number = 1; number <= SEATS_PER_ROW; number++) {
                Seat seat = new Seat();
                seat.setShow(show);
                seat.setSeatNumber(rowLetters[row] + String.valueOf(number));
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setPrice(DEFAULT_PRICE);
                seats.add(seat);
            }
        }

        seatRepository.saveAll(seats);
    }
}