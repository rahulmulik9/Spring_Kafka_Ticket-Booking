package com.rahul.bookingservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Booking's own copy of the message that Cinema publishes.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SeatsReservedEvent {

    private String eventId;
    private Long bookingId;
    private Long userId;
    private Long showId;
    private String movieName;
    private LocalDateTime showTime;
    private BigDecimal totalAmount;
    private List<SeatDetail> seats;
}