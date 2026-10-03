package com.rahul.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// Payment only needs the booking, the user and the money. It ignores the movie and seat fields in the message.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SeatsReservedEvent {

    private String eventId;
    private Long bookingId;
    private Long userId;
    private BigDecimal totalAmount;
}