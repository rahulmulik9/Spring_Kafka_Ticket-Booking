package com.rahul.paymentservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentCompletedEvent {

    private String eventId;
    private Long bookingId;
    private Long paymentId;
    private BigDecimal amount;
}