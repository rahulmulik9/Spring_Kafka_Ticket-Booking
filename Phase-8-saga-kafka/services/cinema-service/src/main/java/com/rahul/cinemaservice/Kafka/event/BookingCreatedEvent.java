package com.rahul.cinemaservice.Kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookingCreatedEvent {

    private String eventId;
    private Long bookingId;
    private Long userId;
    private Long showId;
    private List<Long> seatIds;
}