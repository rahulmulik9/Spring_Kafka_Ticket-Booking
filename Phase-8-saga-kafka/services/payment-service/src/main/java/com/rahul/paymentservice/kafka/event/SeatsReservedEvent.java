package com.rahul.paymentservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

// showId and seats are needed now: if the payment fails, we must tell everyone which seats to release.
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
    private BigDecimal totalAmount;
    private List<SeatDetail> seats;
}