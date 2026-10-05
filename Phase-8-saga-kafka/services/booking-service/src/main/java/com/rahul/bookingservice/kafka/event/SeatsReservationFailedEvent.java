package com.rahul.bookingservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SeatsReservationFailedEvent {

    private String eventId;
    private Long bookingId;
    private String reason;
    private String movieName;
    private LocalDateTime showTime;
    private List<SeatDetail> seats;
}