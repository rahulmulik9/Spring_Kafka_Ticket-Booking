package com.rahul.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient email must be a valid email address")
    private String recipientEmail;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Booking id is required")
    private Long bookingId;
}