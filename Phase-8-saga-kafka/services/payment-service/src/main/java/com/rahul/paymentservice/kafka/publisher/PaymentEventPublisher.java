package com.rahul.paymentservice.kafka.publisher;

import com.rahul.paymentservice.kafka.config.KafkaTopicConfig;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        String key = String.valueOf(event.getBookingId());

        kafkaTemplate.send(KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC, key, event)
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