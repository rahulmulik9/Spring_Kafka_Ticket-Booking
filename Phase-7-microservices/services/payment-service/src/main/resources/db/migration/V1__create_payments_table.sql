CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    booking_id     BIGINT         NOT NULL,
    user_id        BIGINT         NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payments_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);