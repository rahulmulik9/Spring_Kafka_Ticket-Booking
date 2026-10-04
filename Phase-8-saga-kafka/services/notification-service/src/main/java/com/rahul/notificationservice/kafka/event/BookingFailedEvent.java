package com.rahul.notificationservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookingFailedEvent {

    private String eventId;
    private Long bookingId;
    private String customerEmail;
    private String reason;
}