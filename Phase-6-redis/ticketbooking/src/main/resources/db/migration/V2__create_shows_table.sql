CREATE TABLE shows (
    id         BIGSERIAL PRIMARY KEY,
    movie_id   BIGINT NOT NULL REFERENCES movies(id),
    show_time  TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_shows_movie_id ON shows(movie_id);