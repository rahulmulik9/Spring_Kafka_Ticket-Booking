-- Existing bookings are test data with no owner. Wipe them and free their seats,
-- so user_id can be NOT NULL from day one.
DELETE FROM booking_seats;
DELETE FROM bookings;
UPDATE seats SET status = 'AVAILABLE';

ALTER TABLE bookings ADD COLUMN user_id BIGINT NOT NULL REFERENCES users(id);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);