CREATE TABLE processed_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(36) NOT NULL UNIQUE,   -- the unique rule is what makes the guard safe
    processed_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);