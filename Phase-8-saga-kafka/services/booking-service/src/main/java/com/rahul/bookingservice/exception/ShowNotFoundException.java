package com.rahul.bookingservice.exception;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(String message) {
        super(message);
    }
}