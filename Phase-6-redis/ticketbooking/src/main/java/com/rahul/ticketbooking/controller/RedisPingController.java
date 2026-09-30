package com.rahul.ticketbooking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

// TEMPORARY: used only to prove Redis works. Delete after testing.
@RestController
@RequiredArgsConstructor
public class RedisPingController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/redis-ping")
    public String ping() {
        redisTemplate.opsForValue().set("ping", "pong", Duration.ofSeconds(60));
        return redisTemplate.opsForValue().get("ping");
    }
}