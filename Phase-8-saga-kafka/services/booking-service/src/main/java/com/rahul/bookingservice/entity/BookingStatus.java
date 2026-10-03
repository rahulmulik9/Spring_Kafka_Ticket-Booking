package com.rahul.bookingservice.entity;

public enum BookingStatus {
    PENDING,          // saved, waiting for payment
    CONFIRMED,        // paid
    CANCELLED,        // cancelled by the customer
    PAYMENT_FAILED    // payment declined, seats released
}