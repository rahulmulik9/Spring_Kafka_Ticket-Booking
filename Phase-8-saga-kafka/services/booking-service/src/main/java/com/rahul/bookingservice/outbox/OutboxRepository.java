package com.rahul.bookingservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // Oldest first, so events are sent in the order they were created.
    List<OutboxEvent> findTop50ByStatusOrderByIdAsc(OutboxStatus status);
}