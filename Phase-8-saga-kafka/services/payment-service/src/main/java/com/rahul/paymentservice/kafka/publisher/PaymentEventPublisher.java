package com.rahul.paymentservice.kafka.publisher;

import com.rahul.paymentservice.kafka.config.KafkaTopicConfig;
import com.rahul.paymentservice.kafka.event.PaymentCompletedEvent;
import com.rahul.paymentservice.kafka.event.PaymentFailedEvent;
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
        send(KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC, event.getBookingId(), event.getEventId(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        send(KafkaTopicConfig.PAYMENT_FAILED_TOPIC, event.getBookingId(), event.getEventId(), event);
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