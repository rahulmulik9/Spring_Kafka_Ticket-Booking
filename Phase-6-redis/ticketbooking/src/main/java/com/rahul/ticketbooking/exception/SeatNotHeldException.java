package com.rahul.ticketbooking.exception;

public class SeatNotHeldException extends RuntimeException {
    public SeatNotHeldException(String message) {
        super(message);
    }
}