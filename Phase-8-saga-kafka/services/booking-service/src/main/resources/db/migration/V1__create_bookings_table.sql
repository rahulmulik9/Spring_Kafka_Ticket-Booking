CREATE TABLE bookings (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT         NOT NULL,   -- user-service owns users, so this is only a number
    show_id        BIGINT         NOT NULL,   -- cinema-service owns shows, so this is only a number
    movie_name     VARCHAR(255)   NOT NULL,   -- copied from cinema-service when the booking is made
    show_time      TIMESTAMP      NOT NULL,   -- copied too
    customer_email VARCHAR(255)   NOT NULL,
    total_amount   NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'PAYMENT_FAILED'))
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);