package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.MovieShowCountResponse;
import com.rahul.ticketbooking.dto.MovieSummaryResponse;
import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Movie createMovie(@RequestBody Movie movie) {
        return movieService.createMovie(movie);
    }

    @GetMapping
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }

    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    // TEMPORARY: used to reproduce N+1 in Phase 3
    @GetMapping("/with-show-count")
    public List<MovieShowCountResponse> getMoviesWithShowCount(@RequestParam(required = false) Integer limit) {
        return movieService.getMoviesWithShowCount(limit);
    }

    @GetMapping("/summary/all")
    public List<MovieSummaryResponse> getMovieSummaries() {
        return movieService.getMovieSummaries();
    }

    @GetMapping("/summary")
    public Page<MovieSummaryResponse> getMovieSummaryPage(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return movieService.getMovieSummaryPage(pageable);
    }

}