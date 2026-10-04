CREATE TABLE idempotency_keys (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    idem_key     VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64)  NOT NULL,   -- fingerprint of the request, to catch "same key, different request"
    booking_id   BIGINT       NOT NULL REFERENCES bookings(id),   -- the saved result
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, idem_key)
);

CREATE INDEX idx_idempotency_created_at ON idempotency_keys(created_at);