package com.rahul.cinemaservice.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private String eventId;

    private String topic;

    @Column(name = "message_key")
    private String messageKey;

    @Column(length = 4000)
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}