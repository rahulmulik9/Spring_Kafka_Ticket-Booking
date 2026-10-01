package com.rahul.ticketbooking.movie.service;

import com.rahul.ticketbooking.movie.dto.MovieResponse;
import com.rahul.ticketbooking.movie.dto.MovieShowCountResponse;
import com.rahul.ticketbooking.movie.dto.MovieSummaryResponse;
import com.rahul.ticketbooking.common.dto.PageResponse;
import com.rahul.ticketbooking.movie.entity.Movie;
import com.rahul.ticketbooking.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    // A new movie changes the total count and can change any page,
    // so we remove all pages of this cache.
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @CacheEvict(cacheNames = "movieSummaryPage", allEntries = true)
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

    // Cached by id. A missing movie returns null, and that "empty result" is cached for 1 minute
    // (see CacheConfig), so repeated requests for a bad id never reach the database.
    @Cacheable(cacheNames = "movieById", key = "#id", sync = true)
    @Transactional(readOnly = true)
    public MovieResponse getMovieDetails(Long id) {
        log.info("CACHE MISS - loading movie {} from DB", id);

        Optional<Movie> moveis = movieRepository.findById(id);
        MovieResponse responseMoe = new MovieResponse(moveis.get().getId(),moveis.get().getName(), moveis.get().getDescription() );

        return responseMoe;
    }

    //reproduces N+1 => use List<Movie> movies = movieRepository.findAll();
    //fixed N+1 => use List<Movie> movies = movieRepository.findAllWithShows();
    @Transactional(readOnly = true)
    public List<MovieShowCountResponse> getMoviesWithShowCount(Integer limit) {

        // BEFORE (N+1): findAll() loads only the movies (1 query).
        // The shows stay lazy, so each movie.getShows().size() in the loop fired one extra query.
        // 10,002 movies = 10,003 queries, ~15 s.
        // List<Movie> movies = movieRepository.findAll();

        // AFTER (fix): findAllWithShows() uses "left join fetch m.shows",
        // so movies and their shows come back together in 1 query.
        // The entity stays LAZY. Only this query asks for the shows.
        List<Movie> movies = movieRepository.findAllWithShows();

        if (limit != null) {
            movies = movies.stream().limit(limit).toList();
        }

        List<MovieShowCountResponse> result = new ArrayList<>();
        for (Movie movie : movies) {
            // Shows are already loaded by the fetch join, so .size() runs no query here
            int showCount = movie.getShows().size();
            result.add(new MovieShowCountResponse(movie.getId(), movie.getName(), showCount));
        }
        return result;
    }


    /// Only to fetch required data
    @Transactional(readOnly = true)
    public List<MovieSummaryResponse> getMovieSummaries() {
        return movieRepository.findAllSummaries();
    }

    // sync = true: on a stampede only one request rebuilds the entry, the others wait and reuse it
    @Cacheable(cacheNames = "movieSummaryPage", sync = true,
            key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true)
    public PageResponse<MovieSummaryResponse> getMovieSummaryPage(Pageable pageable) {
        log.info("CACHE MISS - loading summary page from DB");
        Page<MovieSummaryResponse> page = movieRepository.findSummaryPage(pageable);
        return new PageResponse<>(
                new ArrayList<>(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }


    @Transactional(readOnly = true)
    public Page<MovieSummaryResponse> searchMoviesByName(String name, Pageable pageable) {
        return movieRepository.searchByName(name, pageable);
    }

}