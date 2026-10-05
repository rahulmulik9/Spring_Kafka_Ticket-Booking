package com.rahul.paymentservice.kafka.publisher;

import com.rahul.paymentservice.outbox.OutboxEvent;
import com.rahul.paymentservice.outbox.OutboxService;
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

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        for (OutboxEvent row : outboxService.findUnsent()) {
            try {
                JsonNode payload = jsonMapper.readTree(row.getPayload());

                SendResult<String, Object> result = kafkaTemplate
                        .send(row.getTopic(), row.getMessageKey(), payload)
                        .get(5, TimeUnit.SECONDS);

                outboxService.markSent(row.getId());

                log.info("Sent event {} to {} partition {} offset {}", row.getEventId(), row.getTopic(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } catch (Exception ex) {
                // Kafka is down or slow. The row stays PENDING. Stop, so a later event never overtakes an earlier one.
                log.error("Could not send outbox event {} to {}. Will retry. Reason: {}",
                        row.getEventId(), row.getTopic(), ex.getMessage());
                return;
            }
        }
    }
}