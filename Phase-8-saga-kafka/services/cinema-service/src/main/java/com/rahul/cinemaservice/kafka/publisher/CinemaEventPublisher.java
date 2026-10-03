package com.rahul.cinemaservice.kafka.publisher;

import com.rahul.cinemaservice.kafka.config.KafkaTopicConfig;
import com.rahul.cinemaservice.kafka.event.SeatsReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CinemaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSeatsReserved(SeatsReservedEvent event) {
        String key = String.valueOf(event.getBookingId());

        kafkaTemplate.send(KafkaTopicConfig.SEATS_RESERVED_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event {}", event.getEventId(), ex);
                    } else {
                        log.info("Sent event {} to partition {} offset {}", event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}