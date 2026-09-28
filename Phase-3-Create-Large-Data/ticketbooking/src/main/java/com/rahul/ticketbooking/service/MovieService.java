package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.rahul.ticketbooking.dto.MovieShowCountResponse;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + id));
    }

    // TEMPORARY: reproduces N+1. We replace this in Step 4.
    @Transactional(readOnly = true)
    public List<MovieShowCountResponse> getMoviesWithShowCount(Integer limit) {
        List<Movie> movies = movieRepository.findAll();          // query 1
        if (limit != null) {
            movies = movies.stream().limit(limit).toList();
        }

        List<MovieShowCountResponse> result = new ArrayList<>();
        for (Movie movie : movies) {
            // getShows() is LAZY, so .size() triggers one extra query per movie
            int showCount = movie.getShows().size();             // queries 2..N+1
            result.add(new MovieShowCountResponse(movie.getId(), movie.getName(), showCount));
        }
        return result;
    }

}