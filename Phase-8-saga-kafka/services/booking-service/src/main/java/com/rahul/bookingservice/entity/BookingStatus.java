package com.rahul.bookingservice.entity;

public enum BookingStatus {
    PENDING,            // saved, waiting for seats and payment
    CONFIRMED,          // paid
    CANCELLED,          // cancelled by the customer
    PAYMENT_FAILED,     // payment declined, seats released
    SEATS_UNAVAILABLE   // a seat was already taken, nothing was reserved
}