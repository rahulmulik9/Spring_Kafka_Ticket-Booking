package com.rahul.bookingservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResult {

    private Long id;
    private String status;          // SUCCESS or FAILED
    private String failureReason;
}