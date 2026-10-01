package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.SeatHoldResponse;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.SeatStatus;
import com.rahul.ticketbooking.exception.SeatAlreadyBookedException;
import com.rahul.ticketbooking.exception.SeatAlreadyHeldException;
import com.rahul.ticketbooking.exception.SeatNotFoundException;
import com.rahul.ticketbooking.exception.SeatNotHeldException;
import com.rahul.ticketbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    // One key per seat. Example: hold:seat:12  ->  "7"  (value = id of the user who holds it)
    private static final String KEY_PREFIX = "hold:seat:";

    private final StringRedisTemplate redisTemplate;
    private final SeatRepository seatRepository;

    @Value("${seat-hold.ttl-seconds:300}")
    private long holdTtlSeconds;

    @PreAuthorize("isAuthenticated()")
    public SeatHoldResponse holdSeats(Long showId, List<Long> requestedIds, Long userId) {

        List<Long> seatIds = requestedIds.stream().distinct().toList();

        // 1. The database is the truth about what exists and what is already sold
        List<Seat> seats = seatRepository.findByIdInAndShowId(seatIds, showId);
        if (seats.size() != seatIds.size()) {
            throw new SeatNotFoundException("One or more seats do not exist for this show");
        }
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatAlreadyBookedException("Seat " + seat.getSeatNumber() + " is already booked");
            }
        }

        // 2. Try to take each seat in Redis
        String owner = String.valueOf(userId);
        List<String> takenInThisCall = new ArrayList<>();

        for (Seat seat : seats) {
            String key = KEY_PREFIX + seat.getId();

            // SET key value NX EX ttl: "set only if it does not exist, and expire it".
            // It is ONE atomic command, so two users can never both succeed.
            Boolean taken = redisTemplate.opsForValue().setIfAbsent(key, owner, Duration.ofSeconds(holdTtlSeconds));

            if (Boolean.TRUE.equals(taken)) {
                takenInThisCall.add(key);
                continue;
            }

            // The key already exists. If it is ours (user clicked twice), that is fine.
            if (owner.equals(redisTemplate.opsForValue().get(key))) {
                continue;
            }

            // Someone else holds it: undo what we took in this call, so the request is all-or-nothing
            if (!takenInThisCall.isEmpty()) {
                redisTemplate.delete(takenInThisCall);
            }
            throw new SeatAlreadyHeldException("Seat " + seat.getSeatNumber() + " is held by another user");
            // Try to create Redis key
            // If created → we got the seat, remember it
            // If not created → key already exists
            // Then Check who owns the key
            // If it's us → already holding the seat, continue
            // If it's another user → rollback seats we got earlier and throw error
        }

        log.info("User {} holds seats {} of show {} for {} seconds", userId, seatIds, showId, holdTtlSeconds);
        return new SeatHoldResponse(showId, seatIds, holdTtlSeconds);
    }

    @PreAuthorize("isAuthenticated()")
    public void releaseSeats(List<Long> seatIds, Long userId) {
        String owner = String.valueOf(userId);

        for (Long seatId : seatIds) {
            String key = KEY_PREFIX + seatId;

            // Only the owner can release. Other people's holds and missing holds are ignored silently.
            if (owner.equals(redisTemplate.opsForValue().get(key))) {
                redisTemplate.delete(key);
            }
        }
        log.info("User {} released seats {}", userId, seatIds);
    }

    // Used by booking. Throws if any seat is not held by this user right now.
    public void assertHeldByUser(List<Long> seatIds, Long userId) {
        String owner = String.valueOf(userId);

        for (Long seatId : seatIds.stream().distinct().toList()) {
            String holder = redisTemplate.opsForValue().get(KEY_PREFIX + seatId);

            if (holder == null) {
                throw new SeatNotHeldException("Seat " + seatId + " is not held. Hold it first, or your hold has expired");
            }
            if (!owner.equals(holder)) {
                throw new SeatNotHeldException("Seat " + seatId + " is held by another user");
            }
        }
    }
}