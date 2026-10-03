package com.rahul.bookingservice.client;

import com.rahul.bookingservice.exception.*;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The other services answer errors as {"status":..,"error":"SEAT_ALREADY_BOOKED","message":"..",..}.
// We read the "error" code and turn it back into the exception booking-service already understands.
public class FeignErrorDecoder implements ErrorDecoder {

    private static final Pattern ERROR_CODE = Pattern.compile("\"error\"\\s*:\\s*\"([A-Z_]+)\"");
    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = readBody(response);
        String code = find(ERROR_CODE, body);
        String message = find(MESSAGE, body);

        if (code == null) {
            return new DependencyFailedException(methodKey + " returned HTTP " + response.status());
        }

        return switch (code) {
            case "SEAT_ALREADY_BOOKED" -> new SeatAlreadyBookedException(message);
            case "SEAT_DOES_NOT_BELONG_TO_SHOW" -> new SeatDoesNotBelongToShowException(message);
            case "SEAT_NOT_FOUND" -> new SeatNotFoundException(message);
            case "SHOW_NOT_FOUND" -> new ShowNotFoundException(message);
            case "SEAT_LOCK_TIMEOUT" -> new SeatBusyException(message);
            case "PAYMENT_ALREADY_DONE" -> new PaymentAlreadyDoneException(message);
            default -> new DependencyFailedException(
                    methodKey + " returned HTTP " + response.status() + " (" + code + ")");
        };
    }

    private String readBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try {
            return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private String find(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}