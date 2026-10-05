package com.rahul.bookingservice.exception;

public class SeatBusyException extends RuntimeException {

    public SeatBusyException(String message) {
        super(message);
    }
}