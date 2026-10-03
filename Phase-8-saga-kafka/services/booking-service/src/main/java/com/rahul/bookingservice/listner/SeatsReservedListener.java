package com.rahul.bookingservice.listner;

import com.rahul.bookingservice.event.SeatsReservedEvent;
import com.rahul.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatsReservedListener {

    private final BookingService bookingService;

    @KafkaListener(topics = "seats-reserved", properties = "spring.json.value.default.type=com.rahul.bookingservice.event.SeatsReservedEvent")
    public void onSeatsReserved(@Payload SeatsReservedEvent event) {
        log.info("Received {}", event);
        bookingService.addReservationDetails(event);
    }
}