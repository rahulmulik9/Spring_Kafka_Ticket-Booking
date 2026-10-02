package com.rahul.bookingservice.exception;

public class DependencyFailedException extends RuntimeException {

    public DependencyFailedException(String message) {
        super(message);
    }
}