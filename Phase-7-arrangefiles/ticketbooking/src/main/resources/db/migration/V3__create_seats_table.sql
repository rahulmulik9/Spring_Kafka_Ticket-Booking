CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    show_id     BIGINT NOT NULL REFERENCES shows(id),
    seat_number VARCHAR(10) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    price       NUMERIC(10, 2) NOT NULL,
    UNIQUE (show_id, seat_number)
);

CREATE INDEX idx_seats_show_id ON seats(show_id);