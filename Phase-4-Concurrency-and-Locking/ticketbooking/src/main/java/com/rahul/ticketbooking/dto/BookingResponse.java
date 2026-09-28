package com.rahul.ticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long showId;
    private String movieTitle;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String status;
    private List<String> seatNumbers;
    private LocalDateTime createdAt;
}