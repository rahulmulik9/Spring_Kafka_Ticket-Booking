package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.exception.SeatAlreadyBookedException;
import com.rahul.cinemaservice.repository.MovieRepository;
import com.rahul.cinemaservice.repository.SeatRepository;
import com.rahul.cinemaservice.repository.ShowRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NOT @Transactional on purpose: every thread needs its own real transaction.
// The test creates its own data and removes it afterwards.
@SpringBootTest
class ConcurrentReservationTest {

    private static final int THREADS = 10;

    @Autowired
    private MovieService movieService;
    @Autowired
    private ShowService showService;
    @Autowired
    private com.rahul.cinemaservice.service.SeatReservationService reservationService;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private SeatRepository seatRepository;

    private Movie movie;
    private Show show;

    @AfterEach
    void cleanUp() {
        if (show != null) {
            seatRepository.deleteAll(seatRepository.findByShowId(show.getId()));
            showRepository.deleteById(show.getId());
        }
        if (movie != null) {
            movieRepository.deleteById(movie.getId());
        }
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void tenCustomersGrabTheSameSeatAndExactlyOneWins() throws Exception {
        Movie newMovie = new Movie();
        newMovie.setName("Concurrency Test Movie");
        movie = movieService.createMovie(newMovie);

        Show newShow = new Show();
        newShow.setShowTime(LocalDateTime.now().plusDays(3));
        show = showService.createShow(movie.getId(), newShow);

        Long showId = show.getId();
        Long seatId = seatRepository.findByShowId(showId).get(0).getId();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger won = new AtomicInteger();
        AtomicInteger turnedAway = new AtomicInteger();
        AtomicInteger otherErrors = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    reservationService.reserveSeats(showId, List.of(seatId));
                    won.incrementAndGet();
                } catch (SeatAlreadyBookedException ex) {
                    turnedAway.incrementAndGet();
                } catch (Exception ex) {
                    otherErrors.incrementAndGet();
                }
            });
        }

        ready.await();   // every thread is waiting at the starting line
        go.countDown();  // fire them all at once
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        System.out.printf("won=%d, turnedAway=%d, otherErrors=%d%n", won.get(), turnedAway.get(), otherErrors.get());

        assertEquals(1, won.get());
        assertEquals(THREADS - 1, turnedAway.get());   // losers get the clean "already booked" answer
        assertEquals(0, otherErrors.get());
    }
}