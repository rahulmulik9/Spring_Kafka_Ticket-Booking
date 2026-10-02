package com.rahul.bookingservice.client;

import com.rahul.bookingservice.client.dto.SeatIdsRequest;
import com.rahul.bookingservice.client.dto.SeatReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// A blank url means "find cinema-service by name through Eureka".
@FeignClient(name = "cinema-service", url = "${services.cinema.url:}")
public interface CinemaClient {

    @PostMapping("/internal/shows/{showId}/seats/reserve")
    SeatReservationResponse reserveSeats(@PathVariable("showId") Long showId,
                                         @RequestBody SeatIdsRequest request);

    @PostMapping("/internal/shows/{showId}/seats/release")
    void releaseSeats(@PathVariable("showId") Long showId,
                      @RequestBody SeatIdsRequest request);
}