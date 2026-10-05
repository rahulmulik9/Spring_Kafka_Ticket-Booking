package com.rahul.cinemaservice.exception;

public class SeatDoesNotBelongToShowException extends RuntimeException {
    public SeatDoesNotBelongToShowException(String message) {
        super(message);
    }
}