package com.rahul.ticketbooking.exception;

public class SeatBusyException extends RuntimeException {
    public SeatBusyException(String message) {
        super(message);
    }
}