package com.rahul.ticketbooking.concurrency;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.service.BookingFacade;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/*
 * Step 7: Compare optimistic vs pessimistic locking.
 *  - HEAVY: 30 threads, 1 seat (worst case contention).
 *  - LIGHT: 30 threads, 30 different seats (no real contention, measures pure overhead).
 *  - Not asserting pass/fail here, just measuring and printing. Read the numbers.
 */
@Slf4j
@ActiveProfiles("dev")
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=60")
class LockComparisonTest {

    private static final int THREADS = 30;
    private static final String PREFIX = "cmp-";

    @Autowired
    private BookingFacade bookingFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long showId;
    private List<Long> seatIds;

    @BeforeEach
    void setUp() {
        cleanUp();
        showId = jdbcTemplate.queryForObject(
                "SELECT show_id FROM seats ORDER BY id LIMIT 1", Long.class);
        seatIds = jdbcTemplate.queryForList(
                "SELECT id FROM seats WHERE show_id = ? ORDER BY id LIMIT " + THREADS, Long.class, showId);
        resetSeats();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        resetSeats();
    }

    @Test
    void heavyContention_pessimistic() throws Exception {
        runHeavy("PESSIMISTIC (heavy)", bookingFacade::createBooking);
    }

    @Test
    void heavyContention_optimistic() throws Exception {
        runHeavy("OPTIMISTIC (heavy)", bookingFacade::createBookingOptimistic);
    }

    @Test
    void lightContention_pessimistic() throws Exception {
        runLight("PESSIMISTIC (light)", bookingFacade::createBooking);
    }

    @Test
    void lightContention_optimistic() throws Exception {
        runLight("OPTIMISTIC (light)", bookingFacade::createBookingOptimistic);
    }

    // All threads fight for seatIds.get(0)
    private void runHeavy(String label, BiFunction<Long, BookingRequest, ?> bookFn) throws Exception {
        Long theSeat = seatIds.get(0);
        run(label, THREADS, i -> List.of(theSeat));
    }

    // Each thread gets its own seat, so nobody actually collides
    private void runLight(String label, BiFunction<Long, BookingRequest, ?> bookFn) throws Exception {
        run(label, THREADS, i -> List.of(seatIds.get(i)));
    }

    private void run(String label, int threads, java.util.function.IntFunction<List<Long>> seatsForThread)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        boolean pessimistic = label.startsWith("PESSIMISTIC");

        for (int i = 0; i < threads; i++) {
            final int userNumber = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    BookingRequest request = new BookingRequest();
                    request.setCustomerName("Cmp User " + userNumber);
                    request.setCustomerEmail(PREFIX + label.hashCode() + "-" + userNumber + "@test.com");
                    request.setSeatIds(seatsForThread.apply(userNumber));
                    if (pessimistic) {
                        bookingFacade.createBooking(showId, request);
                    } else {
                        bookingFacade.createBookingOptimistic(showId, request);
                    }
                    success.incrementAndGet();
                } catch (Exception ex) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        long startedAt = System.currentTimeMillis();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startedAt;
        executor.shutdown();

        log.info("===== {} =====", label);
        log.info("Success : {}", success.get());
        log.info("Rejected: {}", rejected.get());
        log.info("Time(ms): {}", elapsed);
        log.info("=========================");
    }

    private void resetSeats() {
        for (Long id : seatIds) {
            jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id = ?", id);
        }
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", PREFIX + "%");
    }
}