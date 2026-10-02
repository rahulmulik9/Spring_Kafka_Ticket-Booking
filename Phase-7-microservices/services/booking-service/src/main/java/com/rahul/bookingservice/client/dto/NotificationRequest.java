package com.rahul.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private String recipientEmail;
    private String type;            // BOOKING_CONFIRMED, BOOKING_CANCELLED or PAYMENT_FAILED
    private Long bookingId;
}