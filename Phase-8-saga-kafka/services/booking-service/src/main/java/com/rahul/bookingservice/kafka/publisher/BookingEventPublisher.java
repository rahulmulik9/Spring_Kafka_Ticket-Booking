package com.rahul.bookingservice.kafka.publisher;

import com.rahul.bookingservice.kafka.config.KafkaTopicConfig;
import com.rahul.bookingservice.kafka.event.BookingConfirmedEvent;
import com.rahul.bookingservice.kafka.event.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    // Object, because this publisher now sends two different event classes
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingCreated(BookingCreatedEvent event) {
        send(KafkaTopicConfig.BOOKING_CREATED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        send(KafkaTopicConfig.BOOKING_CONFIRMED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    private void send(String topic, Long bookingId, String eventId, Object event) {
        String key = String.valueOf(bookingId);   // same booking, same partition, same order

        kafkaTemplate.send(topic, key, event)
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