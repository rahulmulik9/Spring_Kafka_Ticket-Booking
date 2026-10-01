package com.rahul.ticketbooking.seat.exception;

public class SeatBusyException extends RuntimeException {
    public SeatBusyException(String message) {
        super(message);
    }
}