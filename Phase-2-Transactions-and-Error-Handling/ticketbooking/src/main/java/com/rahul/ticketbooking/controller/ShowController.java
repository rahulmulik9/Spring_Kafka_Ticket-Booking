package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.entity.Show;
import com.rahul.ticketbooking.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies/{movieId}/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Show createShow(@PathVariable Long movieId, @RequestBody Show show) {
        return showService.createShow(movieId, show);
    }

    @GetMapping
    public List<Show> getShowsByMovie(@PathVariable Long movieId) {
        return showService.getShowsByMovie(movieId);
    }
}