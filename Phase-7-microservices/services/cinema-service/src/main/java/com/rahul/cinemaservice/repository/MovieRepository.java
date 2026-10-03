package com.rahul.cinemaservice.repository;

import com.rahul.cinemaservice.dto.MovieSummaryResponse;
import com.rahul.cinemaservice.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Movies together with their shows in ONE query (fetch join), so no N+1 when counting shows.
    @Query("select m from Movie m left join fetch m.shows")
    List<Movie> findAllWithShows();

    // Only id and name are selected. No entity, no description, no createdAt.
    @Query("select new com.rahul.cinemaservice.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m order by m.id")
    List<MovieSummaryResponse> findAllSummaries();

    // One page only. Sort, limit and offset come from the Pageable.
    @Query(value = "select new com.rahul.cinemaservice.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m",
            countQuery = "select count(m) from Movie m")
    Page<MovieSummaryResponse> findSummaryPage(Pageable pageable);

    // Exact name match, one page at a time. Uses the index on movies.name.
    @Query(value = "select new com.rahul.cinemaservice.dto.MovieSummaryResponse(m.id, m.name) " +
            "from Movie m where m.name = :name",
            countQuery = "select count(m) from Movie m where m.name = :name")
    Page<MovieSummaryResponse> searchByName(@Param("name") String name, Pageable pageable);
}