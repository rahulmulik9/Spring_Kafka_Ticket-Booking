package com.rahul.notificationservice.event;

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
public class BookingCreatedEvent {

    private String eventId;
    private Long bookingId;
    private Long userId;
    private BigDecimal totalAmount;
}