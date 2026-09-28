package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.dto.MovieSummaryResponse;
import com.rahul.ticketbooking.entity.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Option 1: fetch join written by hand in JPQL
    @Query("select m from Movie m left join fetch m.shows")
    List<Movie> findAllWithShows();

    // Option 2: same result, join declared with an entity graph
    @EntityGraph(attributePaths = "shows")
    @Query("select m from Movie m")
    List<Movie> findAllWithShowsGraph();

    // Only id and name are selected. No entities, no description, no createdAt.
    @Query("select new com.rahul.ticketbooking.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m order by m.id")
    List<MovieSummaryResponse> findAllSummaries();
}
