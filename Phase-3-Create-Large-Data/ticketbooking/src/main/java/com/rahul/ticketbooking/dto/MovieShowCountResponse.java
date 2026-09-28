package com.rahul.ticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MovieShowCountResponse {

    private Long id;
    private String name;
    private int showCount;
}