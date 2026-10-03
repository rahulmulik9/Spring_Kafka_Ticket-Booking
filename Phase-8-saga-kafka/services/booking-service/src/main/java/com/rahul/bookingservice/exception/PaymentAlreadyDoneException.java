package com.rahul.bookingservice.exception;

public class PaymentAlreadyDoneException extends RuntimeException {

    public PaymentAlreadyDoneException(String message) {
        super(message);
    }
}