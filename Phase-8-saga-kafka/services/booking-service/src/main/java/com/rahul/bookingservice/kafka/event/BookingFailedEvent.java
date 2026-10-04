package com.rahul.bookingservice.kafka.event;

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
public class BookingFailedEvent {

    private String eventId;
    private Long bookingId;
    private Long showId;
    private List<Long> seatIds;   // empty when no seat was ever reserved
    private String customerEmail;
    private String reason;
}