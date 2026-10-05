package com.rahul.bookingservice.kafka.publisher;

import com.rahul.bookingservice.outbox.OutboxEvent;
import com.rahul.bookingservice.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    // Every second: send the unsent rows, oldest first.
    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        for (OutboxEvent row : outboxService.findUnsent()) {
            try {
                // Send the stored JSON as a JSON tree, so the serializer writes it as a normal object.
                JsonNode payload = jsonMapper.readTree(row.getPayload());

                // Wait for Kafka's answer. Only a confirmed send may be marked SENT.
                SendResult<String, Object> result = kafkaTemplate
                        .send(row.getTopic(), row.getMessageKey(), payload)
                        .get(5, TimeUnit.SECONDS);

                outboxService.markSent(row.getId());

                log.info("Sent event {} to {} partition {} offset {}", row.getEventId(), row.getTopic(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } catch (Exception ex) {
                // Kafka is down or slow. The row stays PENDING and the next run tries again.
                // Stop here, so a later event never overtakes an earlier one.
                log.error("Could not send outbox event {} to {}. Will retry. Reason: {}",
                        row.getEventId(), row.getTopic(), ex.getMessage());
                return;
            }
        }
    }
}