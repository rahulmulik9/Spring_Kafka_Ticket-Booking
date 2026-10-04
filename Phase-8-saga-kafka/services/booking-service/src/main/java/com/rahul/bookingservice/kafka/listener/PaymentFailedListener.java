package com.rahul.bookingservice.kafka.listener;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.entity.BookingStatus;
import com.rahul.bookingservice.kafka.event.BookingFailedEvent;
import com.rahul.bookingservice.kafka.event.PaymentFailedEvent;
import com.rahul.bookingservice.kafka.publisher.BookingEventPublisher;
import com.rahul.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedListener {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @KafkaListener(topics = "payment-failed",
            properties = "spring.json.value.default.type=com.rahul.bookingservice.kafka.event.PaymentFailedEvent")
    public void onPaymentFailed(@Payload PaymentFailedEvent event) {
        log.info("Received {}", event);

        Booking booking = bookingService.updateStatus(event.getBookingId(), BookingStatus.PAYMENT_FAILED);

        eventPublisher.publishBookingFailed(new BookingFailedEvent(
                UUID.randomUUID().toString(),
                booking.getId(),
                event.getShowId(),
                event.getSeatIds(),
                booking.getCustomerEmail(),
                event.getReason()));
    }
}