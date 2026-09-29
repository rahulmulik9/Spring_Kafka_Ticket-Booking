-- Two more shows for existing movies
INSERT INTO shows (movie_id, show_time) VALUES
    (1, '2026-09-26 18:00:00'),
    (1, '2026-09-26 21:00:00'),
    (2, '2026-09-27 19:00:00');

-- Generate 50 seats (A1-E10) for each of the 3 shows above
INSERT INTO seats (show_id, seat_number, status, price)
SELECT s.id,
       chr(65 + (n / 10)) || ((n % 10) + 1)::text,
       'AVAILABLE',
       250.00
FROM shows s
CROSS JOIN generate_series(0, 49) AS n
WHERE s.id IN (
    SELECT id FROM shows ORDER BY id DESC LIMIT 3
);