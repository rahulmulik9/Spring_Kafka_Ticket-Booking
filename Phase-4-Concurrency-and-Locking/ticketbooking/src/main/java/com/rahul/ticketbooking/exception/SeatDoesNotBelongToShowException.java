package com.rahul.ticketbooking.exception;

public class SeatDoesNotBelongToShowException extends RuntimeException {
    public SeatDoesNotBelongToShowException(String message) {
        super(message);
    }
}