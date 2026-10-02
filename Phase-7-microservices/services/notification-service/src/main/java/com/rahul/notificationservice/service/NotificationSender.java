package com.rahul.notificationservice.service;

// One way of delivering a message. Today it logs. Later it could be email or SMS.
public interface NotificationSender {

    void send(String recipientEmail, String message);
}