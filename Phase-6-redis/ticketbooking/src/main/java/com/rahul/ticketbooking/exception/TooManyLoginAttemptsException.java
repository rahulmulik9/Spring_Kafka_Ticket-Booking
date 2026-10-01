package com.rahul.ticketbooking.exception;

import lombok.Getter;

@Getter
public class TooManyLoginAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}