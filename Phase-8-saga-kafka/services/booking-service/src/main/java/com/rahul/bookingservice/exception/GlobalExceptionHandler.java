package com.rahul.bookingservice.exception;

import com.rahul.bookingservice.dto.ErrorResponse;
import com.rahul.bookingservice.idempotency.IdempotencyKeyReuseException;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ShowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShowNotFound(ShowNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "SHOW_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(SeatNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSeatNotFound(SeatNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(SeatAlreadyBookedException.class)
    public ResponseEntity<ErrorResponse> handleSeatBooked(SeatAlreadyBookedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "SEAT_ALREADY_BOOKED", ex.getMessage(), request);
    }

    @ExceptionHandler(SeatDoesNotBelongToShowException.class)
    public ResponseEntity<ErrorResponse> handleWrongShow(SeatDoesNotBelongToShowException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "SEAT_DOES_NOT_BELONG_TO_SHOW", ex.getMessage(), request);
    }

    @ExceptionHandler(BookingAlreadyCancelledException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyCancelled(BookingAlreadyCancelledException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "BOOKING_ALREADY_CANCELLED", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidBookingStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_BOOKING_STATE", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentAlreadyDoneException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyDone(PaymentAlreadyDoneException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "PAYMENT_ALREADY_DONE", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(SeatBusyException.class)
    public ResponseEntity<ErrorResponse> handleSeatBusy(SeatBusyException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SEAT_LOCK_TIMEOUT",
                "The seat is busy right now. Please try again in a few seconds.", request);
    }

    // A service we call answered with an error we do not understand.
    @ExceptionHandler(DependencyFailedException.class)
    public ResponseEntity<ErrorResponse> handleDependencyFailed(DependencyFailedException ex, HttpServletRequest request) {
        log.error("Dependency failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "DEPENDENCY_FAILED",
                "A service we depend on returned an error. Please try again later.", request);
    }

    // A service we call could not be reached at all (stopped, wrong port, network).
    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ErrorResponse> handleDependencyDown(RetryableException ex, HttpServletRequest request) {
        log.error("Dependency unreachable at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
                "A service we depend on is not reachable. Please try again later.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to perform this action.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or not valid JSON.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Something went wrong. Please try again later.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(status.value(), errorCode, message,
                request.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyKeyReuse(IdempotencyKeyReuseException ex) {
        log.warn("Idempotency key reused with a different request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "status", 422,
                "error", "IDEMPOTENCY_KEY_REUSED",
                "message", ex.getMessage()));
    }

    // Header missing: this should be 400, not 500.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "MISSING_HEADER",
                "message", "Required header '" + ex.getHeaderName() + "' is missing"));
    }
}