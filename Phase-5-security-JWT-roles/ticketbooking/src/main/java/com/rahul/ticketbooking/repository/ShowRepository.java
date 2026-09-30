// repository/ShowRepository.java
package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieId(Long eventId);

    @Query("select s from Show s join fetch s.movie where s.id = :id")
    Optional<Show> findByIdWithMovie(@Param("id") Long id);
}