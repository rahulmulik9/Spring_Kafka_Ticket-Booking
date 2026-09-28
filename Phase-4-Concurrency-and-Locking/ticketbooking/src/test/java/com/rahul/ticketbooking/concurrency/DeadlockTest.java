package com.rahul.ticketbooking.concurrency;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Step 6: Deadlock guard.
 *  - User A asks for seats [A, B], user B asks for [B, A] at the same moment.
 *  - findAllByIdForUpdate has ORDER BY id, so both lock the lower id first. One waits, no cycle.
 *  - Expected every round: exactly 1 OK, exactly 1 REJECTED, no errors.
 *  - A deadlock needs locks taken in separate statements in opposite order (see the psql demo).
 */
@Slf4j
@ActiveProfiles("dev")
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=10")
class DeadlockTest {

    private static final int ROUNDS = 20;
    private static final String PREFIX = "dead-";

    @Autowired
    private BookingFacade bookingFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long showId;
    private Long seatA;
    private Long seatB;

    @BeforeEach
    void setUp() {
        cleanUpBookings();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, show_id FROM seats ORDER BY id LIMIT 1");
        showId = ((Number) row.get("show_id")).longValue();

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM seats WHERE show_id = ? ORDER BY id LIMIT 2", Long.class, showId);
        seatA = ids.get(0);
        seatB = ids.get(1);
        resetSeats();
    }

    @AfterEach
    void tearDown() {
        cleanUpBookings();
        resetSeats();
    }

    @Test
    void oppositeSeatOrderMustNotDeadlock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int ok = 0;
        int rejected = 0;
        int errors = 0;

        for (int round = 0; round < ROUNDS; round++) {
            cleanUpBookings();
            resetSeats();

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            final String suffix = "-" + round;

            Future<String> first = executor.submit(() -> book(ready, start, "dead-a" + suffix, List.of(seatA, seatB)));
            Future<String> second = executor.submit(() -> book(ready, start, "dead-b" + suffix, List.of(seatB, seatA)));

            ready.await();
            start.countDown();

            for (String result : List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS))) {
                if (result.equals("OK")) {
                    ok++;
                } else if (result.equals("REJECTED")) {
                    rejected++;
                } else {
                    errors++;
                    log.error("Round {}: {}", round, result);
                }
            }
        }
        executor.shutdown();

        log.info("===== RESULT =====");
        log.info("Rounds   : {}", ROUNDS);
        log.info("OK       : {}", ok);
        log.info("REJECTED : {}", rejected);
        log.info("ERRORS   : {}", errors);
        log.info("==================");

        assertEquals(0, errors, "No deadlock or lock errors expected");
        assertEquals(ROUNDS, ok, "Exactly one winner per round");
        assertEquals(ROUNDS, rejected, "Exactly one clean rejection per round");
    }

    private String book(CountDownLatch ready, CountDownLatch start, String email, List<Long> seatIds)
            throws InterruptedException {
        ready.countDown();
        start.await();

        BookingRequest request = new BookingRequest();
        request.setCustomerName("Deadlock Test");
        request.setCustomerEmail(email + "@test.com");
        request.setSeatIds(seatIds);
        try {
            bookingFacade.createBooking(showId, request);
            return "OK";
        } catch (SeatAlreadyBookedException ex) {
            return "REJECTED";
        } catch (Exception ex) {
            return "ERROR: " + ex.getClass().getSimpleName();
        }
    }

    private void resetSeats() {
        jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id IN (?, ?)", seatA, seatB);
    }

    private void cleanUpBookings() {
        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", PREFIX + "%");
    }
}