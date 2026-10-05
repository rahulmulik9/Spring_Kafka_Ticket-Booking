package com.rahul.paymentservice.kafka.listener;

import com.rahul.paymentservice.kafka.event.SeatsReservedEvent;
import com.rahul.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "seats-reserved")
    public void onSeatsReserved(@Payload SeatsReservedEvent event) {
        log.info("Received {}", event);
        paymentService.payForBooking(event);
    }
}