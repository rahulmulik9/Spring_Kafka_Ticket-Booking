package com.rahul.cinemaservice.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markIfNew(String eventId) {
        return processedEventRepository.insertIfAbsent(eventId) == 1;
    }
}