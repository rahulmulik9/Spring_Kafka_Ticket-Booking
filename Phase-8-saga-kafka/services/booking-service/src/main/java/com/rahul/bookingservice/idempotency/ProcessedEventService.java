package com.rahul.bookingservice.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    // MANDATORY: must run inside the caller's transaction, so the record and the work
    // are saved together or rolled back together.
    // Returns true if this is the first time we see the event, false if it is a repeat.
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markIfNew(String eventId) {
        return processedEventRepository.insertIfAbsent(eventId) == 1;
    }
}