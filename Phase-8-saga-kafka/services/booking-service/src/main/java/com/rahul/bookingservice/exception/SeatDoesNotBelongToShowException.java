package com.rahul.bookingservice.exception;

public class SeatDoesNotBelongToShowException extends RuntimeException {

    public SeatDoesNotBelongToShowException(String message) {
        super(message);
    }
}