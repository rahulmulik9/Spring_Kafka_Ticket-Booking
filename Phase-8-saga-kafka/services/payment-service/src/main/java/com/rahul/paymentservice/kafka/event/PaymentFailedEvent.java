package com.rahul.paymentservice.kafka.event;

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
public class PaymentFailedEvent {

    private String eventId;
    private Long bookingId;
    private Long showId;
    private List<Long> seatIds;   // carried along, so Booking does not depend on its own copy being filled in yet
    private String reason;
}