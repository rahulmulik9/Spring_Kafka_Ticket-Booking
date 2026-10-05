CREATE TABLE outbox_events (
    id          BIGSERIAL PRIMARY KEY,
    event_id    VARCHAR(36)   NOT NULL UNIQUE,
    topic       VARCHAR(100)  NOT NULL,
    message_key VARCHAR(100)  NOT NULL,
    payload     VARCHAR(4000) NOT NULL,   -- the event as JSON
    status      VARCHAR(10)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMP,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT'))
);

CREATE INDEX idx_outbox_status_id ON outbox_events(status, id);