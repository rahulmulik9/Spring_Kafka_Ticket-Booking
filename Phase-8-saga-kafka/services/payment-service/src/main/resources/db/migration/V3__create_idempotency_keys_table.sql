CREATE TABLE idempotency_keys (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    idem_key     VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64)  NOT NULL,
    payment_id   BIGINT       NOT NULL REFERENCES payments(id),   -- the saved result
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, idem_key)
);

CREATE INDEX idx_idempotency_created_at ON idempotency_keys(created_at);

-- Last line of defense: whatever the code does, a booking can never have two SUCCESS payments.
CREATE UNIQUE INDEX uq_payments_booking_success ON payments(booking_id) WHERE status = 'SUCCESS';