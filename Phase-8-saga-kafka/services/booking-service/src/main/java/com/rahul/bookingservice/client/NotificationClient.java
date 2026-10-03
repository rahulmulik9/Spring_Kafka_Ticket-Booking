package com.rahul.bookingservice.client;

import com.rahul.bookingservice.client.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${services.notification.url:}")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    void send(@RequestBody NotificationRequest request);
}