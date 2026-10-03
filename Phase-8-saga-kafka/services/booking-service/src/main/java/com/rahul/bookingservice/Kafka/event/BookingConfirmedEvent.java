package com.rahul.bookingservice.Kafka.event;

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
public class BookingConfirmedEvent {

    private String eventId;
    private Long bookingId;
    private String customerEmail;   // Notification has no way to look up the email, so it travels in the message
}