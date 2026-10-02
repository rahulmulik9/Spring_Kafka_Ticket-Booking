package com.rahul.bookingservice.exception;

public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(Long bookingId, String reason) {
        super("Payment for booking " + bookingId + " failed: " + reason);
    }
}