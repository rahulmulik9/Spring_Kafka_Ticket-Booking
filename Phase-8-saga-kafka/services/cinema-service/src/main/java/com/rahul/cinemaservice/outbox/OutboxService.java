package com.rahul.cinemaservice.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    // MANDATORY: must run inside the caller's transaction, so the event and the data are saved together.
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(String topic, Long bookingId, String eventId, Object event) {
        OutboxEvent row = new OutboxEvent();
        row.setEventId(eventId);
        row.setTopic(topic);
        row.setMessageKey(String.valueOf(bookingId));
        row.setPayload(jsonMapper.writeValueAsString(event));
        row.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> findUnsent() {
        return outboxRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING);
    }

    @Transactional
    public void markSent(Long id) {
        OutboxEvent row = outboxRepository.findById(id).orElseThrow();
        row.setStatus(OutboxStatus.SENT);
        row.setSentAt(LocalDateTime.now());
    }
}