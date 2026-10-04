package com.rahul.bookingservice.kafka.publisher;

import com.rahul.bookingservice.kafka.config.KafkaTopicConfig;
import com.rahul.bookingservice.kafka.event.BookingCancelledEvent;
import com.rahul.bookingservice.kafka.event.BookingConfirmedEvent;
import com.rahul.bookingservice.kafka.event.BookingCreatedEvent;
import com.rahul.bookingservice.kafka.event.BookingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingCreated(BookingCreatedEvent event) {
        send(KafkaTopicConfig.BOOKING_CREATED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        send(KafkaTopicConfig.BOOKING_CONFIRMED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    public void publishBookingFailed(BookingFailedEvent event) {
        send(KafkaTopicConfig.BOOKING_FAILED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    public void publishBookingCancelled(BookingCancelledEvent event) {
        send(KafkaTopicConfig.BOOKING_CANCELLED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    private void send(String topic, Long bookingId, String eventId, Object event) {
        kafkaTemplate.send(topic, String.valueOf(bookingId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event {} to {}", eventId, topic, ex);
                    } else {
                        log.info("Sent event {} to {} partition {} offset {}", eventId, topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}