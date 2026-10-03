package com.rahul.cinemaservice.repository;

import com.rahul.cinemaservice.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowId(Long showId);

    // SELECT ... FOR UPDATE: the first transaction locks these seat rows and others wait.
    // The lock is released when the transaction ends, so the caller must be @Transactional.
    // ORDER BY id: every request locks seats in the same order, which prevents deadlocks.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id in :ids order by s.id")
    List<Seat> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}