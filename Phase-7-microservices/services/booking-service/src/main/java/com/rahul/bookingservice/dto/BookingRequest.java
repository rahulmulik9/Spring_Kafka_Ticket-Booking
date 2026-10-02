package com.rahul.bookingservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;
}