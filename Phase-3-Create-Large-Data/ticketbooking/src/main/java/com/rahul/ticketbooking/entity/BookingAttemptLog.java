package com.rahul.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_attempt_log")
@Getter
@Setter
public class BookingAttemptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long showId;

    private String customerEmail;

    private boolean successful;

    @Column(length = 500)
    private String failureReason;

    private LocalDateTime attemptedAt;
}