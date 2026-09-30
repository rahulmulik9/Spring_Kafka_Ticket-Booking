///*
//* Step 1: Multithreaded test
//*  - 30 threads try to book the SAME seat at the same instant.
//*  - Three latches: ready (all at the line), start (the gun), done (all finished).
//*  - No @Transactional on this test: each thread must run its own real transaction and commit.
//*  - Pool size raised to 60 so we see a booking race, not connection timeouts.
// */
//
//
//
//
////package com.rahul.ticketbooking.concurrency;
////
////import com.rahul.ticketbooking.dto.BookingRequest;
////import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
////import com.rahul.ticketbooking.service.BookingService;
////import lombok.extern.slf4j.Slf4j;
////import org.junit.jupiter.api.AfterEach;
////import org.junit.jupiter.api.BeforeEach;
////import org.junit.jupiter.api.Test;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.boot.test.context.SpringBootTest;
////import org.springframework.jdbc.core.JdbcTemplate;
////import org.springframework.test.context.ActiveProfiles;
////
////import java.util.List;
////import java.util.Map;
////import java.util.concurrent.CountDownLatch;
////import java.util.concurrent.ExecutorService;
////import java.util.concurrent.Executors;
////import java.util.concurrent.TimeUnit;
////import java.util.concurrent.atomic.AtomicInteger;
////
////@Slf4j
////@ActiveProfiles("dev")
////@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=60")
////class DoubleBookingTest {
////
////    private static final int THREADS = 30;
////    private static final String TEST_EMAIL_PREFIX = "race-";
////
////    @Autowired
////    private BookingService bookingService;
////
////    @Autowired
////    private JdbcTemplate jdbcTemplate;
////
////    private Long showId;
////    private Long seatId;
////
////    @BeforeEach
////    void setUp() {
////        cleanUpTestBookings();
////
////        Map<String, Object> row = jdbcTemplate.queryForMap(
////                "SELECT id, show_id FROM seats ORDER BY id LIMIT 1");
////        seatId = ((Number) row.get("id")).longValue();
////        showId = ((Number) row.get("show_id")).longValue();
////
////        resetSeat();
////    }
////
////    @AfterEach
////    void tearDown() {
////        cleanUpTestBookings();
////        resetSeat();
////    }
////
////    @Test
////    void manyUsersBookTheSameSeat() throws Exception {
////        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
////        CountDownLatch ready = new CountDownLatch(THREADS);   // every thread reached the gate
////        CountDownLatch start = new CountDownLatch(1);         // the starting gun
////        CountDownLatch done = new CountDownLatch(THREADS);    // every thread finished
////
////        AtomicInteger success = new AtomicInteger();
////        AtomicInteger alreadyBooked = new AtomicInteger();
////        AtomicInteger otherErrors = new AtomicInteger();
////
////        for (int i = 0; i < THREADS; i++) {
////            final int userNumber = i;
////            executor.submit(() -> {
////                try {
////                    ready.countDown();
////                    start.await();   // wait here until the gun fires
////
////                    BookingRequest request = new BookingRequest();
////                    request.setCustomerName("Race User " + userNumber);
////                    request.setCustomerEmail(TEST_EMAIL_PREFIX + userNumber + "@test.com");
////                    request.setSeatIds(List.of(seatId));
////
////                    bookingService.createBooking(showId, request);
////                    success.incrementAndGet();
////                } catch (SeatAlreadyBookedException ex) {
////                    alreadyBooked.incrementAndGet();
////                } catch (Exception ex) {
////                    otherErrors.incrementAndGet();
////                    log.error("Unexpected error: {}", ex.toString());
////                } finally {
////                    done.countDown();
////                }
////            });
////        }
////
////        ready.await();       // all threads are lined up
////        start.countDown();   // fire
////        done.await(30, TimeUnit.SECONDS);
////        executor.shutdown();
////
////        Integer bookingsForSeat = jdbcTemplate.queryForObject(
////                "SELECT COUNT(*) FROM booking_seats bs "
////                        + "JOIN bookings b ON b.id = bs.booking_id "
////                        + "WHERE bs.seat_id = ? AND b.customer_email LIKE ?",
////                Integer.class, seatId, TEST_EMAIL_PREFIX + "%");
////
////        log.info("===== RESULT =====");
////        log.info("Threads fired            : {}", THREADS);
////        log.info("Successful bookings      : {}", success.get());
////        log.info("'Already booked' errors  : {}", alreadyBooked.get());
////        log.info("Other errors             : {}", otherErrors.get());
////        log.info("Booking rows for the seat: {}", bookingsForSeat);
////        log.info("==================");
////    }
////
////    private void resetSeat() {
////        jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id = ?", seatId);
////    }
////
////    private void cleanUpTestBookings() {
////        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
////                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", TEST_EMAIL_PREFIX + "%");
////        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", TEST_EMAIL_PREFIX + "%");
////    }
////}
//
//
//
//
//
//
//
//
//
//
//
//
//
///*
// * Step 2: Observe the failure
// *  - Added assertions: exactly 1 success and exactly 1 booking row.
// *  - Before any locking: 30 successes, 30 booking rows, seat status BOOKED (looks normal!).
// *  - Cause: check (status == AVAILABLE) and update (status = BOOKED) are separate steps.
// *    Default isolation is READ COMMITTED, so every thread read AVAILABLE before anyone committed.
// *
// */
//
//
//
///*
//package com.rahul.ticketbooking.concurrency;
//
//import com.rahul.ticketbooking.dto.BookingRequest;
//import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
//import com.rahul.ticketbooking.service.BookingService;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.RepeatedTest;
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
//@Slf4j
//@ActiveProfiles("dev")
//@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=60")
//class DoubleBookingTest {
//
//    private static final int THREADS = 30;
//    private static final String TEST_EMAIL_PREFIX = "race-";
//
//    @Autowired
//    private BookingService bookingService;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    private Long showId;
//    private Long seatId;
//
//    @BeforeEach
//    void setUp() {
//        cleanUpTestBookings();
//
//        Map<String, Object> row = jdbcTemplate.queryForMap(
//                "SELECT id, show_id FROM seats ORDER BY id LIMIT 1");
//        seatId = ((Number) row.get("id")).longValue();
//        showId = ((Number) row.get("show_id")).longValue();
//
//        resetSeat();
//    }
//
//    @AfterEach
//    void tearDown() {
//        cleanUpTestBookings();
//        resetSeat();
//    }
//
//    @RepeatedTest(3)
//    void manyUsersBookTheSameSeat() throws Exception {
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
//                    request.setCustomerName("Race User " + userNumber);
//                    request.setCustomerEmail(TEST_EMAIL_PREFIX + userNumber + "@test.com");
//                    request.setSeatIds(List.of(seatId));
//
//                    bookingService.createBooking(showId, request);
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
//        start.countDown();
//        boolean finished = done.await(30, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        Integer bookingsForSeat = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM booking_seats bs "
//                        + "JOIN bookings b ON b.id = bs.booking_id "
//                        + "WHERE bs.seat_id = ? AND b.customer_email LIKE ?",
//                Integer.class, seatId, TEST_EMAIL_PREFIX + "%");
//
//        String seatStatus = jdbcTemplate.queryForObject(
//                "SELECT status FROM seats WHERE id = ?", String.class, seatId);
//
//        log.info("===== RESULT =====");
//        log.info("Threads fired            : {}", THREADS);
//        log.info("Successful bookings      : {}", success.get());
//        log.info("'Already booked' errors  : {}", alreadyBooked.get());
//        log.info("Other errors             : {}", otherErrors.get());
//        log.info("Booking rows for the seat: {}", bookingsForSeat);
//        log.info("Seat status in database  : {}", seatStatus);
//        log.info("==================");
//
//        assertTrue(finished, "Threads did not finish within 30 seconds");
//        assertEquals(1, success.get(), "Only one user should get the seat");
//        assertEquals(1, bookingsForSeat, "Only one booking row should exist for the seat");
//    }
//
//    private void resetSeat() {
//        jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id = ?", seatId);
//    }
//
//    private void cleanUpTestBookings() {
//        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
//                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", TEST_EMAIL_PREFIX + "%");
//        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", TEST_EMAIL_PREFIX + "%");
//    }
//}
//*/
//
//
//
//
//
//
//
//
//
//
//
//
//
//
///*
// * Step 3: Optimistic locking
// *  - Added @Version on Seat. Result: 1 success, 29 ObjectOptimisticLockingFailureException.
// *  - Added the "conflicts" counter and catch block to count this new type of failure.
// *
// */
//
//
///*
//package com.rahul.ticketbooking.concurrency;
//
//import com.rahul.ticketbooking.dto.BookingRequest;
//import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
//import com.rahul.ticketbooking.service.BookingService;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.RepeatedTest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
//@Slf4j
//@ActiveProfiles("dev")
//@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=60")
//class DoubleBookingTest {
//
//    private static final int THREADS = 30;
//    private static final String TEST_EMAIL_PREFIX = "race-";
//
//    @Autowired
//    private BookingService bookingService;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    private Long showId;
//    private Long seatId;
//
//    @BeforeEach
//    void setUp() {
//        cleanUpTestBookings();
//
//        Map<String, Object> row = jdbcTemplate.queryForMap(
//                "SELECT id, show_id FROM seats ORDER BY id LIMIT 1");
//        seatId = ((Number) row.get("id")).longValue();
//        showId = ((Number) row.get("show_id")).longValue();
//
//        resetSeat();
//    }
//
//    @AfterEach
//    void tearDown() {
//        cleanUpTestBookings();
//        resetSeat();
//    }
//
//    @RepeatedTest(3)
//    void manyUsersBookTheSameSeat() throws Exception {
//        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
//        CountDownLatch ready = new CountDownLatch(THREADS);
//        CountDownLatch start = new CountDownLatch(1);
//        CountDownLatch done = new CountDownLatch(THREADS);
//
//        AtomicInteger success = new AtomicInteger();
//        AtomicInteger alreadyBooked = new AtomicInteger();
//        AtomicInteger conflicts = new AtomicInteger();
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
//                    request.setCustomerName("Race User " + userNumber);
//                    request.setCustomerEmail(TEST_EMAIL_PREFIX + userNumber + "@test.com");
//                    request.setSeatIds(List.of(seatId));
//
//                    bookingService.createBooking(showId, request);
//                    success.incrementAndGet();
//                } catch (SeatAlreadyBookedException ex) {
//                    alreadyBooked.incrementAndGet();
//                } catch (ObjectOptimisticLockingFailureException ex) {
//                    conflicts.incrementAndGet();
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
//        start.countDown();
//        boolean finished = done.await(30, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        Integer bookingsForSeat = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM booking_seats bs "
//                        + "JOIN bookings b ON b.id = bs.booking_id "
//                        + "WHERE bs.seat_id = ? AND b.customer_email LIKE ?",
//                Integer.class, seatId, TEST_EMAIL_PREFIX + "%");
//
//        String seatStatus = jdbcTemplate.queryForObject(
//                "SELECT status FROM seats WHERE id = ?", String.class, seatId);
//
//        log.info("===== RESULT =====");
//        log.info("Threads fired            : {}", THREADS);
//        log.info("Successful bookings      : {}", success.get());
//        log.info("'Already booked' errors  : {}", alreadyBooked.get());
//        log.info("Optimistic lock conflicts: {}", conflicts.get());
//        log.info("Other errors             : {}", otherErrors.get());
//        log.info("Booking rows for the seat: {}", bookingsForSeat);
//        log.info("Seat status in database  : {}", seatStatus);
//        log.info("==================");
//
//        assertTrue(finished, "Threads did not finish within 30 seconds");
//        assertEquals(1, success.get(), "Only one user should get the seat");
//        assertEquals(1, bookingsForSeat, "Only one booking row should exist for the seat");
//        assertEquals(THREADS, success.get() + alreadyBooked.get() + conflicts.get(),
//                "Every thread must end as success, already booked, or conflict");
//    }
//
//    private void resetSeat() {
//        jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id = ?", seatId);
//    }
//
//    private void cleanUpTestBookings() {
//        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
//                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", TEST_EMAIL_PREFIX + "%");
//        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", TEST_EMAIL_PREFIX + "%");
//    }
//}
//
//*/
//
//
//
//
//
//
///*
// * Step 4: Handle the conflict
// *  - Test now calls BookingFacade (retry) instead of BookingService.
// *  - Result: 1 success, 29 "already booked", 0 lock conflicts.
// *    Losers fail on attempt 1, retry, re-read the seat, and see BOOKED on attempt 2.
//
// */
//
//
//
//
//
//package com.rahul.ticketbooking.concurrency;
//
//import com.rahul.ticketbooking.dto.BookingRequest;
//import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
//import com.rahul.ticketbooking.service.BookingFacade;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.RepeatedTest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
//@Slf4j
//@ActiveProfiles("dev")
//@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=60")
//class DoubleBookingTest {
//
//    private static final int THREADS = 30;
//    private static final String TEST_EMAIL_PREFIX = "race-";
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
//        cleanUpTestBookings();
//
//        Map<String, Object> row = jdbcTemplate.queryForMap(
//                "SELECT id, show_id FROM seats ORDER BY id LIMIT 1");
//        seatId = ((Number) row.get("id")).longValue();
//        showId = ((Number) row.get("show_id")).longValue();
//
//        resetSeat();
//    }
//
//    @AfterEach
//    void tearDown() {
//        cleanUpTestBookings();
//        resetSeat();
//    }
//
//    @RepeatedTest(3)
//    void manyUsersBookTheSameSeat() throws Exception {
//        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
//        CountDownLatch ready = new CountDownLatch(THREADS);
//        CountDownLatch start = new CountDownLatch(1);
//        CountDownLatch done = new CountDownLatch(THREADS);
//
//        AtomicInteger success = new AtomicInteger();
//        AtomicInteger alreadyBooked = new AtomicInteger();
//        AtomicInteger conflicts = new AtomicInteger();
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
//                    request.setCustomerName("Race User " + userNumber);
//                    request.setCustomerEmail(TEST_EMAIL_PREFIX + userNumber + "@test.com");
//                    request.setSeatIds(List.of(seatId));
//
//                    bookingFacade.createBooking(showId, request);
//                    success.incrementAndGet();
//                } catch (SeatAlreadyBookedException ex) {
//                    alreadyBooked.incrementAndGet();
//                } catch (ObjectOptimisticLockingFailureException ex) {
//                    conflicts.incrementAndGet();
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
//        start.countDown();
//        boolean finished = done.await(30, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        Integer bookingsForSeat = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM booking_seats bs "
//                        + "JOIN bookings b ON b.id = bs.booking_id "
//                        + "WHERE bs.seat_id = ? AND b.customer_email LIKE ?",
//                Integer.class, seatId, TEST_EMAIL_PREFIX + "%");
//
//        String seatStatus = jdbcTemplate.queryForObject(
//                "SELECT status FROM seats WHERE id = ?", String.class, seatId);
//
//        log.info("===== RESULT =====");
//        log.info("Threads fired            : {}", THREADS);
//        log.info("Successful bookings      : {}", success.get());
//        log.info("'Already booked' errors  : {}", alreadyBooked.get());
//        log.info("Optimistic lock conflicts: {}", conflicts.get());
//        log.info("Other errors             : {}", otherErrors.get());
//        log.info("Booking rows for the seat: {}", bookingsForSeat);
//        log.info("Seat status in database  : {}", seatStatus);
//        log.info("==================");
//
//        assertTrue(finished, "Threads did not finish within 30 seconds");
//        assertEquals(1, success.get(), "Only one user should get the seat");
//        assertEquals(1, bookingsForSeat, "Only one booking row should exist for the seat");
//        assertEquals(THREADS, success.get() + alreadyBooked.get() + conflicts.get(),
//                "Every thread must end as success, already booked, or conflict");
//    }
//
//    private void resetSeat() {
//        jdbcTemplate.update("UPDATE seats SET status = 'AVAILABLE' WHERE id = ?", seatId);
//    }
//
//    private void cleanUpTestBookings() {
//        jdbcTemplate.update("DELETE FROM booking_seats WHERE booking_id IN "
//                + "(SELECT id FROM bookings WHERE customer_email LIKE ?)", TEST_EMAIL_PREFIX + "%");
//        jdbcTemplate.update("DELETE FROM bookings WHERE customer_email LIKE ?", TEST_EMAIL_PREFIX + "%");
//    }
//}
