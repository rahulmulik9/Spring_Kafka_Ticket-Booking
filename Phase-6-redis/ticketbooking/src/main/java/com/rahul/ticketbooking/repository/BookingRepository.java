package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // The mapper needs show and movie after the transaction ends, so load them up front.
    @Query("select b from Booking b join fetch b.show s join fetch s.movie where b.id = :id")
    Optional<Booking> findByIdWithShowAndMovie(@Param("id") Long id);
}