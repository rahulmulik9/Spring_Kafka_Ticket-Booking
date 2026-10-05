package com.rahul.bookingservice.kafka.listener;

import com.rahul.bookingservice.kafka.event.PaymentFailedEvent;
import com.rahul.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedListener {

    private final BookingService bookingService;

    @KafkaListener(topics = "payment-failed",
            properties = "spring.json.value.default.type=com.rahul.bookingservice.kafka.event.PaymentFailedEvent")
    public void onPaymentFailed(@Payload PaymentFailedEvent event) {
        log.info("Received {}", event);
        bookingService.markPaymentFailed(event);
    }
}