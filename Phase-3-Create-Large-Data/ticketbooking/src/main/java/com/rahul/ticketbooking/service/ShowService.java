package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.SeatStatus;
import com.rahul.ticketbooking.entity.Show;
import com.rahul.ticketbooking.repository.SeatRepository;
import com.rahul.ticketbooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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