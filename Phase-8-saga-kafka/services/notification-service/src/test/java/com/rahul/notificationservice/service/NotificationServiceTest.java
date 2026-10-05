package com.rahul.notificationservice.service;

import com.rahul.notificationservice.dto.NotificationRequest;
import com.rahul.notificationservice.dto.NotificationResponse;
import com.rahul.notificationservice.dto.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Plain unit test: no Spring, no database. A small fake sender records what would have been sent.
class NotificationServiceTest {

    private final List<String> recipients = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    private final NotificationService service = new NotificationService((to, message) -> {
        recipients.add(to);
        messages.add(message);
    });

    private NotificationRequest request(NotificationType type) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientEmail("rahul@test.com");
        request.setType(type);
        request.setBookingId(1001L);
        return request;
    }

    @Test
    void confirmedMessageMentionsTheBookingNumber() {
        NotificationResponse response = service.send(request(NotificationType.BOOKING_CONFIRMED));

        assertTrue(response.getMessage().contains("#1001"));
        assertTrue(response.getMessage().contains("confirmed"));
    }

    @Test
    void cancelledMessageSaysSeatsAreReleased() {
        NotificationResponse response = service.send(request(NotificationType.BOOKING_CANCELLED));

        assertTrue(response.getMessage().contains("cancelled"));
        assertTrue(response.getMessage().contains("released"));
    }

    @Test
    void paymentFailedMessageAsksToTryAgain() {
        NotificationResponse response = service.send(request(NotificationType.PAYMENT_FAILED));

        assertTrue(response.getMessage().contains("failed"));
        assertTrue(response.getMessage().contains("try again"));
    }

    @Test
    void messageGoesOutExactlyOnceToTheRecipient() {
        service.send(request(NotificationType.BOOKING_CONFIRMED));

        assertEquals(List.of("rahul@test.com"), recipients);
        assertEquals(1, messages.size());
    }
}