package com.rahul.notificationservice.service;

import com.rahul.notificationservice.dto.NotificationRequest;
import com.rahul.notificationservice.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSender sender;

    public NotificationResponse send(NotificationRequest request) {
        Long bookingId = request.getBookingId();

        String message = switch (request.getType()) {
            case BOOKING_CONFIRMED -> "Your booking #" + bookingId + " is confirmed. Enjoy the show!";
            case BOOKING_CANCELLED -> "Your booking #" + bookingId + " has been cancelled. Your seats have been released.";
            case PAYMENT_FAILED -> "Payment for booking #" + bookingId + " failed. Please try again.";
        };

        sender.send(request.getRecipientEmail(), message);

        return new NotificationResponse(request.getRecipientEmail(), request.getType(), message, "SENT");
    }
}