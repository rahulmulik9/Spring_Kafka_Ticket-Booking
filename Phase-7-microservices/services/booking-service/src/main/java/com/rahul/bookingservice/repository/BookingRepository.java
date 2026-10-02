package com.rahul.bookingservice.repository;

import com.rahul.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // The seats are lazy, so we load them in the same query. The caller reads them after the transaction ends.
    @Query("select b from Booking b left join fetch b.seats where b.id = :id")
    Optional<Booking> findByIdWithSeats(@Param("id") Long id);
}