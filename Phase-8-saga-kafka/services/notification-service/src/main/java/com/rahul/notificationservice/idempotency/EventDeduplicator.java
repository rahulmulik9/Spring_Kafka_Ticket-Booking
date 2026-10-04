package com.rahul.notificationservice.idempotency;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventDeduplicator {

    private static final int MAX_REMEMBERED = 10_000;

    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    // Returns true the first time an event id is seen, false for a repeat.
    public boolean firstTime(String eventId) {
        if (seen.size() > MAX_REMEMBERED) {
            seen.clear();   // keeps memory bounded
        }
        return seen.add(eventId);
    }
}