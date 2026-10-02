package com.rahul.paymentservice.controller;

import com.rahul.paymentservice.dto.PaymentRequest;
import com.rahul.paymentservice.dto.PaymentResponse;
import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.security.AuthUser;
import com.rahul.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Returns 201 for both outcomes. A declined payment is still a payment attempt that was recorded,
    // and the "status" field tells the caller what happened.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request,
                               @AuthenticationPrincipal AuthUser caller) {
        Payment payment = paymentService.pay(request.getBookingId(), caller.getId(), request.getAmount());
        return PaymentResponse.from(payment);
    }

    @GetMapping("/booking/{bookingId}")
    public List<PaymentResponse> getByBooking(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal AuthUser caller) {
        boolean isAdmin = "ADMIN".equals(caller.getRole());
        return paymentService.getPaymentsForCaller(bookingId, caller.getId(), isAdmin).stream()
                .map(PaymentResponse::from)
                .toList();
    }
}