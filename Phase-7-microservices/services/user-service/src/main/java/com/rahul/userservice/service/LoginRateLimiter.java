package com.rahul.userservice.service;

import com.rahul.userservice.exception.TooManyLoginAttemptsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimiter {

    // Example: login:attempts:alice@test.com -> "3"   (expires when the window ends)
    private static final String KEY_PREFIX = "login:attempts:";

    private final StringRedisTemplate redisTemplate;

    @Value("${login-limit.max-attempts:5}")
    private long maxAttempts;

    @Value("${login-limit.window-seconds:300}")
    private long windowSeconds;

    // Called BEFORE the password is checked. Counts this attempt and refuses it if the limit is passed.
    public void checkAndCount(String email) {
        String key = KEY_PREFIX + email;

        long attempts;
        long secondsLeft;

        try {
            // INCR is atomic: two parallel attempts can never read the same number
            Long count = redisTemplate.opsForValue().increment(key);
            attempts = (count == null) ? 0 : count;

            // Fixed window: the first attempt starts the clock, later attempts do not extend it.
            // We check the TTL every time (not only when count == 1), so a crash between INCR and EXPIRE
            // can never leave a counter that never expires.
            Long ttl = redisTemplate.getExpire(key);
            if (ttl == null || ttl < 0) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
                secondsLeft = windowSeconds;
            } else {
                secondsLeft = ttl;
            }
        } catch (DataAccessException ex) {
            // Redis is down. Login must keep working, so we let the attempt through ("fail open").
            log.warn("Login rate limiter skipped because Redis is unavailable: {}", ex.getMessage());
            return;
        }

        if (attempts > maxAttempts) {
            throw new TooManyLoginAttemptsException(
                    "Too many login attempts. Try again in " + Math.max(secondsLeft, 1) + " seconds.",
                    Math.max(secondsLeft, 1));
        }
    }

    // Called after a successful login: the user proved who they are, so the counter starts again from zero
    public void reset(String email) {
        try {
            redisTemplate.delete(KEY_PREFIX + email);
        } catch (DataAccessException ex) {
            log.warn("Could not reset login counter: {}", ex.getMessage());
        }
    }
}