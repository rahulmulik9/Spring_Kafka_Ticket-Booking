package com.rahul.ticketbooking.seat.service;

import com.rahul.ticketbooking.seat.exception.SeatBusyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    // Not the same as the hold key (hold:seat:...). A hold lasts minutes and belongs to the user.
    // A lock lasts milliseconds and only stops two requests from booking at the same moment.
    private static final String LOCK_PREFIX = "lock:seat:";

    private static final long WAIT_SECONDS = 3;    // how long we wait for someone else's lock
    private static final long LEASE_SECONDS = 10;  // the lock deletes itself after this, even if we crash

    private final RedissonClient redissonClient;

    public <T> T executeWithLocks(List<Long> seatIds, Supplier<T> action) {

        // Always lock in the same order (lowest id first), so two requests can never wait on each other
        List<Long> sortedIds = seatIds.stream().distinct().sorted().toList();
        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            for (Long seatId : sortedIds) {
                RLock lock = redissonClient.getLock(LOCK_PREFIX + seatId);

                boolean acquired;
                try {
                    acquired = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SeatBusyException("Interrupted while waiting for seat " + seatId);
                }

                if (!acquired) {
                    throw new SeatBusyException("Seat " + seatId + " is busy, please try again");
                }
                acquiredLocks.add(lock);
            }

            // TEMPORARY: pretend the work is slow, so two app copies can fight for the lock. Remove after testing.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // The booking runs here. Its transaction commits when the booking call returns,
            // which is BEFORE we unlock below. This order is the whole point.
            return action.get();

        } finally {
            // Release in reverse order, even if the booking failed
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                unlockQuietly(acquiredLocks.get(i));
            }
        }
    }

    private void unlockQuietly(RLock lock) {
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException ex) {
            // The lease ran out before we finished, so Redis already removed the lock
            log.warn("Lock {} was no longer held when unlocking (lease expired?)", lock.getName());
        }
    }
}