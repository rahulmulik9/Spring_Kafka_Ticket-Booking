package com.rahul.notificationservice.listner;

import com.rahul.notificationservice.event.BookingCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingEventListener {

    @KafkaListener(topics = "booking-events")
    public void onBookingCreated(@Payload BookingCreatedEvent event,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received {} | key={} partition={} offset={}", event, key, partition, offset);
    }
}