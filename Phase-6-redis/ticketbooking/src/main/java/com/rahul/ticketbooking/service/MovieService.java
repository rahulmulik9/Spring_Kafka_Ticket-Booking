package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.MovieShowCountResponse;
import com.rahul.ticketbooking.dto.MovieSummaryResponse;
import com.rahul.ticketbooking.dto.redis.PageResponse;
import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;


    // A new movie changes the total count and can change any page,
    // so we cannot know which keys are wrong. We remove all pages of this cach
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


    @Transactional(readOnly = true)
    public Page<MovieSummaryResponse> searchMoviesByName(String name, Pageable pageable) {
        return movieRepository.searchByName(name, pageable);
    }



    //pages
//    @Transactional(readOnly = true)
//    public Page<MovieSummaryResponse> getMovieSummaryPage(Pageable pageable) {
//        return movieRepository.findSummaryPage(pageable);
//    }

    // Cache-aside: check Redis first. On a miss, run the method and store the result.
    // Key example in Redis: movieSummaryPage::0-20-id: ASC

    //sync => The limit: this lock works inside one app copy only.
    @Cacheable(cacheNames = "movieSummaryPage", sync = true, key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true)
    public PageResponse<MovieSummaryResponse> getMovieSummaryPage(Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findSummaryPage(pageable);
        return new PageResponse<>(
                new ArrayList<>(page.getContent()),   // plain ArrayList, easy for Jackson to rebuild
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

}