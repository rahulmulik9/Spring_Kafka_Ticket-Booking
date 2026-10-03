package com.rahul.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NotificationResponse {

    private String recipientEmail;
    private NotificationType type;
    private String message;
    private String status;
}