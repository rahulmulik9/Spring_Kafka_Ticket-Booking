//package com.rahul.ticketbooking.concurrency;
//
//import com.rahul.ticketbooking.booking.dto.BookingRequest;
//import com.rahul.ticketbooking.seat.exception.SeatAlreadyBookedException;
//import com.rahul.ticketbooking.booking.service.BookingFacade;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///*
// * Step 6: Pool starvation.
// *  - Pool is only 10 (same as the real dev config), 20 threads fight for one seat.
// *  - Before the fix: the failure audit (REQUIRES_NEW) needed a 2nd connection while the
// *    booking transaction still held the 1st connection and the seat lock.
// *  - After the fix: the audit runs in BookingFacade, after the transaction has ended.
// */
//@Slf4j
//@ActiveProfiles("dev")
//@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=10")
//class PoolStarvationTest {
//
//    private static final int THREADS = 20;
//    private static final String PREFIX = "pool-";
//
//    @Autowired
//    private BookingFacade bookingFacade;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    private Long showId;
//    private Long seatId;
//
//    @BeforeEach
//    void setUp() {
//        cleanUpBookings();
//        Map<String, Object> row = jdbcTemplate.queryForMap(
//                "SELECT id, show_id FROM seats ORDER BY id LIMIT 1");
//        seatId = ((Number) row.get("id")).longValue();
//        showId = ((Number) row.get("show_id")).longValue();
//        resetSeat();
//    }
//
//    @AfterEach
//    void tearDown() {
//        cleanUpBookings();
//        resetSeat();
//    }
//
//    @Test
//    void hotSeatMustNotExhaustThePool() throws Exception {
//        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
//        CountDownLatch ready = new CountDownLatch(THREADS);
//        CountDownLatch start = new CountDownLatch(1);
//        CountDownLatch done = new CountDownLatch(THREADS);
//
//        AtomicInteger success = new AtomicInteger();
//        AtomicInteger alreadyBooked = new AtomicInteger();
//        AtomicInteger otherErrors = new AtomicInteger();
//
//        for (int i = 0; i < THREADS; i++) {
//            final int userNumber = i;
//            executor.submit(() -> {
//                try {
//                    ready.countDown();
//                    start.await();
//
//                    BookingRequest request = new BookingRequest();
//                    request.setCustomerName("Pool User " + userNumber);
//                    request.setCustomerEmail(PREFIX + userNumber + "@test.com");
//                    request.setSeatIds(List.of(seatId));
//
//                    bookingFacade.createBooking(showId, request);
//                    success.incrementAndGet();
//                } catch (SeatAlreadyBookedException ex) {
//                    alreadyBooked.incrementAndGet();
//                } catch (Exception ex) {
//                    otherErrors.incrementAndGet();
//                    log.error("Unexpected error: {}", ex.toString());
//                } finally {
//                    done.countDown();
//                }
//            });
//        }
//
//        ready.await();
//        long startedAt = System.currentTimeMillis();
//        start.countDown();
//        boolean finished = done.await(30, TimeUnit.SECONDS);
//        long elapsedMs = System.currentTimeMillis() - startedAt;
//        executor.shutdownNow();
//
//        log.info("===== RESULT =====");
//        log.info("Threads fired          : {}", THREADS);
//        log.info("Successful bookings    : {}", success.get());
//        log.info("'Already booked' errors: {}", alreadyBooked.get());
//        log.info("Other errors           : {}", otherErrors.get());
//        log.info("Time taken (ms)        : {}", elapsedMs);
//        log.info("==================");
//
//        assertTrue(finished, "Threads did not finish within 30 seconds");
//        assertEquals(1, success.get(), "Only one user should get the seat");
//        assertEquals(THREADS - 1, alreadyBooked.get(), "Everyone else should be cleanly rejected");
//        assertEquals(0, otherErrors.get(), "No connection or lock errors expected");
//    }
//
//    private void resetSeat() {
//        jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id = ?", seatId);
//    }
//
//    private void cleanUpBookings() {
//        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
//                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", PREFIX + "%");
//        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", PREFIX + "%");
//    }
//}