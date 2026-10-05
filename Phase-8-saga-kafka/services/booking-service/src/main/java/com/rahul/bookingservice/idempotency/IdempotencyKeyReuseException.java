package com.rahul.bookingservice.idempotency;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Same key, different request: that is a client bug, so we refuse instead of guessing.
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String message) {
        super(message);
    }
}