package com.rahul.cinemaservice.kafka.event;

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
public class BookingCancelledEvent {

    private String eventId;
    private Long bookingId;
    private Long showId;
    private List<Long> seatIds;
    private String customerEmail;
}