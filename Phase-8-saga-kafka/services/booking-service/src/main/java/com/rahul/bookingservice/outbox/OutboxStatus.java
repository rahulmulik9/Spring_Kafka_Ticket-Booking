package com.rahul.bookingservice.outbox;

public enum OutboxStatus {
    PENDING,   // saved, not yet sent to Kafka
    SENT       // Kafka confirmed it
}