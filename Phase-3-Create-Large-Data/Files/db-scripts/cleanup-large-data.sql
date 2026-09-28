-- Deletes only the seeded data. Your hand-made movies stay.
-- Order matters: children first, parents last (foreign keys).

DELETE FROM booking_seats
WHERE seat_id IN (
    SELECT s.id FROM seats s
    JOIN shows sh ON sh.id = s.show_id
    JOIN movies m ON m.id = sh.movie_id
    WHERE m.name LIKE 'Seed Movie %');

DELETE FROM bookings
WHERE show_id IN (
    SELECT sh.id FROM shows sh
    JOIN movies m ON m.id = sh.movie_id
    WHERE m.name LIKE 'Seed Movie %');

DELETE FROM seats
WHERE show_id IN (
    SELECT sh.id FROM shows sh
    JOIN movies m ON m.id = sh.movie_id
    WHERE m.name LIKE 'Seed Movie %');

DELETE FROM shows
WHERE movie_id IN (SELECT id FROM movies WHERE name LIKE 'Seed Movie %');

DELETE FROM movies WHERE name LIKE 'Seed Movie %';

VACUUM ANALYZE movies;
VACUUM ANALYZE shows;
VACUUM ANALYZE seats;