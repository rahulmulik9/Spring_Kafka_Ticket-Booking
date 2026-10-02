INSERT INTO movies (name, description) VALUES
    ('Inception', 'A mind-bending thriller about dreams within dreams.'),
    ('The Dark Knight', 'Batman faces off against the Joker in Gotham.');

-- Dates are in the future so the shows are still bookable
INSERT INTO shows (movie_id, show_time) VALUES
    (1, '2026-12-20 18:00:00'),
    (1, '2026-12-20 21:00:00'),
    (2, '2026-12-21 19:00:00');

-- 50 seats (A1 to E10) for each show
INSERT INTO seats (show_id, seat_number, status, price)
SELECT s.id,
       chr(65 + (n / 10)) || ((n % 10) + 1)::text,
       'AVAILABLE',
       250.00
FROM shows s
CROSS JOIN generate_series(0, 49) AS n;