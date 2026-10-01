package com.rahul.ticketbooking.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MovieShowCountResponse {

    private Long id;
    private String name;
    private long showCount;
}