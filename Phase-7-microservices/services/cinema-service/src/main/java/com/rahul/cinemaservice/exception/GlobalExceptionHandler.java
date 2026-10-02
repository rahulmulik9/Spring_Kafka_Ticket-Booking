package com.rahul.cinemaservice.exception;

import com.rahul.cinemaservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMovieNotFound(MovieNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", ex.getMessage(), request);
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

    // A request waited longer than lock_timeout for a seat row, or Postgres ended it to break a deadlock.
    // Both are temporary, so 503 with a clear message instead of a raw 500.
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLockFailure(PessimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Lock failure at {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SEAT_LOCK_TIMEOUT",
                "The seat is busy right now. Please try again in a few seconds.", request);
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
}