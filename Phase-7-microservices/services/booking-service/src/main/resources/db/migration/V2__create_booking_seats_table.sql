CREATE TABLE booking_seats (
    id          BIGSERIAL PRIMARY KEY,
    booking_id  BIGINT         NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id     BIGINT         NOT NULL,      -- cinema-service owns seats, so no foreign key
    seat_number VARCHAR(10)    NOT NULL,      -- copied, so we can show "A1" without asking cinema-service
    price       NUMERIC(10, 2) NOT NULL,      -- the price actually charged for this seat
    UNIQUE (booking_id, seat_id)
);