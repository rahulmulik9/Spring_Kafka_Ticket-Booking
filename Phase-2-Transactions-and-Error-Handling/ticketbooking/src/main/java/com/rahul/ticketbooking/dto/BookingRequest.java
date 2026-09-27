package com.rahul.ticketbooking.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {
    private List<Long> seatIds;
    private String customerName;
    private String customerEmail;
}