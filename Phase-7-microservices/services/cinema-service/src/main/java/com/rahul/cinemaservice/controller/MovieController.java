package com.rahul.cinemaservice.controller;

import com.rahul.cinemaservice.dto.MovieResponse;
import com.rahul.cinemaservice.dto.MovieSummaryResponse;
import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.service.MovieService;
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

    @GetMapping("/summary/all")
    public List<MovieSummaryResponse> getMovieSummaries() {
        return movieService.getMovieSummaries();
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return movieService.getMovieDetails(id);
    }

    @GetMapping("/search")
    public Page<MovieSummaryResponse> searchMovies(
            @RequestParam String name,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return movieService.searchMoviesByName(name, pageable);
    }
}