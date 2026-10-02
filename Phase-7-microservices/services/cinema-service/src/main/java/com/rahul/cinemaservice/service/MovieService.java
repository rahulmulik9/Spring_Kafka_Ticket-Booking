package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.dto.MovieResponse;
import com.rahul.cinemaservice.dto.MovieSummaryResponse;
import com.rahul.cinemaservice.entity.Movie;
import com.rahul.cinemaservice.exception.MovieNotFoundException;
import com.rahul.cinemaservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public MovieResponse getMovieDetails(Long id) {
        return movieRepository.findById(id)
                .map(m -> new MovieResponse(m.getId(), m.getName(), m.getDescription()))
                .orElseThrow(() -> new MovieNotFoundException(id));
    }

    // Only id and name are fetched, not the whole movie row.
    @Transactional(readOnly = true)
    public List<MovieSummaryResponse> getMovieSummaries() {
        return movieRepository.findAllSummaries();
    }

    @Transactional(readOnly = true)
    public Page<MovieSummaryResponse> searchMoviesByName(String name, Pageable pageable) {
        return movieRepository.searchByName(name, pageable);
    }
}