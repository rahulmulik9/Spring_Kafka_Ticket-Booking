-- The booking is now saved before Cinema answers, so these are empty at first.
ALTER TABLE bookings ALTER COLUMN movie_name   DROP NOT NULL;
ALTER TABLE bookings ALTER COLUMN show_time    DROP NOT NULL;
ALTER TABLE bookings ALTER COLUMN total_amount DROP NOT NULL;