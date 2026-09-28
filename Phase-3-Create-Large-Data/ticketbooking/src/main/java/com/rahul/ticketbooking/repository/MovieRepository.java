package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.dto.MovieSummaryResponse;
import com.rahul.ticketbooking.entity.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Option 1: fetch join written by hand in JPQL
    @Query("select m from Movie m left join fetch m.shows")
    List<Movie> findAllWithShows();

    // Option 2: same result, join declared with an entity graph
    @EntityGraph(attributePaths = "shows")
    @Query("select m from Movie m")
    List<Movie> findAllWithShowsGraph();

    // Only id and name are selected. No entity, no description, no createdAt.
    @Query("select new com.rahul.ticketbooking.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m order by m.id")
    List<MovieSummaryResponse> findAllSummaries();

    // returns ONE page. Sort, limit and offset come from the Pageable.
    @Query(value = "select new com.rahul.ticketbooking.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m",
            countQuery = "select count(m) from Movie m")
    Page<MovieSummaryResponse> findSummaryPage(Pageable pageable);

    // Exact name match, one page at a time. Uses the index on movies.name after V-next.
    @Query(value = "select new com.rahul.ticketbooking.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m where m.name = :name",
            countQuery = "select count(m) from Movie m where m.name = :name")
    Page<MovieSummaryResponse> searchByName(@Param("name") String name, Pageable pageable);

}
