CREATE TABLE bookings (
    id             BIGSERIAL PRIMARY KEY,
    show_id        BIGINT NOT NULL REFERENCES shows(id),
    customer_name  VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    total_amount   NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE booking_seats (
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    seat_id    BIGINT NOT NULL REFERENCES seats(id),
    PRIMARY KEY (booking_id, seat_id)
);