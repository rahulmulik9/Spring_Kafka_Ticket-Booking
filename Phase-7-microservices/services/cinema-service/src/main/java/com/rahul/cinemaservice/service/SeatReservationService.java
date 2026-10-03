package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.dto.ReservedSeat;
import com.rahul.cinemaservice.dto.SeatReservationResponse;
import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.entity.SeatStatus;
import com.rahul.cinemaservice.entity.Show;
import com.rahul.cinemaservice.exception.SeatAlreadyBookedException;
import com.rahul.cinemaservice.exception.SeatDoesNotBelongToShowException;
import com.rahul.cinemaservice.exception.SeatNotFoundException;
import com.rahul.cinemaservice.exception.ShowNotFoundException;
import com.rahul.cinemaservice.repository.SeatRepository;
import com.rahul.cinemaservice.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;

    // All or nothing: either every requested seat becomes BOOKED, or none does.
    @Transactional
    public SeatReservationResponse reserveSeats(Long showId, List<Long> seatIds) {
        Show show = showRepository.findByIdWithMovie(showId)
                .orElseThrow(() -> new ShowNotFoundException("Show not found with id: " + showId));

        List<Seat> seats = lockSeats(showId, seatIds);

        // Check every seat first, change nothing until all of them pass.
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatAlreadyBookedException("Seat " + seat.getSeatNumber() + " is already booked");
            }
        }

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        seatRepository.saveAll(seats);

        List<ReservedSeat> reserved = seats.stream()
                .map(s -> new ReservedSeat(s.getId(), s.getSeatNumber(), s.getPrice()))
                .toList();

        return new SeatReservationResponse(show.getId(), show.getMovie().getName(), show.getShowTime(), reserved);
    }

    // Safe to call twice: releasing a seat that is already AVAILABLE changes nothing.
    @Transactional
    public void releaseSeats(Long showId, List<Long> seatIds) {
        if (!showRepository.existsById(showId)) {
            throw new ShowNotFoundException("Show not found with id: " + showId);
        }

        List<Seat> seats = lockSeats(showId, seatIds);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }
        seatRepository.saveAll(seats);
    }

    // SELECT ... FOR UPDATE (ordered by id), then check the seats exist and belong to this show.
    private List<Seat> lockSeats(Long showId, List<Long> seatIds) {
        List<Long> ids = seatIds.stream().distinct().toList();   // [5, 5] must not look like a missing seat

        List<Seat> seats = seatRepository.findAllByIdForUpdate(ids);

        if (seats.size() != ids.size()) {
            throw new SeatNotFoundException("One or more seats do not exist");
        }
        for (Seat seat : seats) {
            if (!seat.getShow().getId().equals(showId)) {
                throw new SeatDoesNotBelongToShowException(
                        "Seat " + seat.getSeatNumber() + " does not belong to this show");
            }
        }
        return seats;
    }
}