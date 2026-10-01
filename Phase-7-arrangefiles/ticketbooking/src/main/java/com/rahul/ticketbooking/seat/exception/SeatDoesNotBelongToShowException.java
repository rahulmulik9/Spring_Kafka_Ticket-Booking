package com.rahul.ticketbooking.seat.exception;

public class SeatDoesNotBelongToShowException extends RuntimeException {
    public SeatDoesNotBelongToShowException(String message) {
        super(message);
    }
}