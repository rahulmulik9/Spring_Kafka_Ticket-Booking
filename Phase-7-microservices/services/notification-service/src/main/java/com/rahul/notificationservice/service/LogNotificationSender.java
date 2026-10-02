package com.rahul.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogNotificationSender implements NotificationSender {

    @Override
    public void send(String recipientEmail, String message) {
        log.info("NOTIFICATION to {}: {}", recipientEmail, message);
    }
}