CREATE TABLE booking_attempt_log (
    id             BIGSERIAL PRIMARY KEY,
    show_id        BIGINT,
    customer_email VARCHAR(255),
    successful     BOOLEAN      NOT NULL,
    failure_reason VARCHAR(500),
    attempted_at   TIMESTAMP    NOT NULL
);