// repository/SeatRepository.java
package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByShowId(Long showId);

    /*
     * Step 5: Pessimistic locking.
     *  - SELECT ... FOR UPDATE: the first transaction locks the seat rows, others wait.
     *  - The lock is released when the transaction ends (commit or rollback),
     *    so @Transactional on createBooking is required.
     *  - After waiting, a loser reads the fresh row, sees BOOKED, and gets SeatAlreadyBookedException.
     *    No retry is needed.
     *  - ORDER BY id: every request locks seats in the same order (prevents deadlocks, Step 6).
     *  - Cost: waiting requests hold DB connections. No lock timeout yet (Step 6).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id in :ids order by s.id")
    List<Seat> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}