package com.rahul.bookingservice.client;

import com.rahul.bookingservice.client.dto.PaymentRequest;
import com.rahul.bookingservice.client.dto.PaymentResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${services.payment.url:}")
public interface PaymentClient {

    @PostMapping("/api/payments")
    PaymentResult pay(@RequestBody PaymentRequest request);
}