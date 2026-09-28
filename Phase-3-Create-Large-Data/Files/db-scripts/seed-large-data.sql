-- Phase 3, Step 1: bulk test data.
-- All seeded movies are named 'Seed Movie N' so we can find and delete them easily.

-- 10,000 movies
INSERT INTO movies (name, description)
SELECT
    'Seed Movie ' || g,
    'Seeded movie number ' || g || ' for performance testing'
FROM generate_series(1, 10000) AS g;

-- 5 shows per seeded movie = 50,000 shows
INSERT INTO shows (movie_id, show_time)
SELECT
    m.id,
    TIMESTAMP '2026-10-01 10:00:00'
        + (s * INTERVAL '3 hours')
        + ((m.id % 30) * INTERVAL '1 day')
FROM movies m
CROSS JOIN generate_series(0, 4) AS s
WHERE m.name LIKE 'Seed Movie %';

-- 10 seats per seeded show = 500,000 seats
INSERT INTO seats (show_id, seat_number, status, price)
SELECT
    sh.id,
    'A' || n,
    'AVAILABLE',
    150 + ((n % 3) * 50)
FROM shows sh
JOIN movies m ON m.id = sh.movie_id
CROSS JOIN generate_series(1, 10) AS n
WHERE m.name LIKE 'Seed Movie %';

-- Refresh planner statistics so later EXPLAIN results are realistic
ANALYZE movies;
ANALYZE shows;
ANALYZE seats;