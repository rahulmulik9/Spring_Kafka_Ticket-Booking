package com.rahul.ticketbooking.seat.exception;

public class SeatNotHeldException extends RuntimeException {
    public SeatNotHeldException(String message) {
        super(message);
    }
}