# Phase 7 Notes

## Step 1: Service boundaries

### Service map

- **user-service (port 8081)**: who you are
  - Tables: `users`, `refresh_tokens`
  - Endpoints: `/api/auth/**`
  - Redis: `login:attempts:*`
  - Moves from the monolith: AuthController, AuthService, UserService, RefreshTokenService,
    JwtService, LoginRateLimiter, DevDataSeeder, User, Role, RefreshToken
  - Only service that creates tokens (holds the signing key for creating)

- **catalog-service (port 8082)**: what can be booked
  - Tables: `movies`, `shows`, `seats`
  - Endpoints: `/movies/**`, `/shows/*/seats`, `/api/holds/**`
  - Redis: `movieById`, `movieSummaryPage` caches, `hold:seat:*`, `lock:seat:*`
  - Moves from the monolith: MovieController, ShowController, SeatController, SeatHoldController,
    MovieService, ShowService, SeatService, SeatHoldService, SeatLockService,
    Movie, Show, Seat, SeatStatus
  - Owns the seat status (AVAILABLE or BOOKED), the `@Version`, the DB row lock and the hold.

- **booking-service (port 8083)**: what a customer bought
  - Tables: `bookings`, `booking_seats`, `booking_attempt_log`
  - Endpoints: `/api/bookings/**`
  - Moves from the monolith: BookingController, BookingService, BookingFacade, AuditService,
    BookingMapper, Booking, BookingStatus, BookingAttemptLog
  - Never touches seat, show, movie or user tables.

- **payment-service (port 8084)**: new, takes payment (simple fake version in Step 7)
- **notification-service (port 8085)**: new, sends messages (log only in Step 7)

Supporting: config-server 8888, eureka 8761, api-gateway 8080.

### Cross-service links to replace

1. `bookings.user_id` -> `users`
   - Today: BookingService loads the User, copies name and email, and checks
     `booking.getUser().getId()` for ownership.
   - Replace with: keep `user_id` as a plain number (no FK, no entity). The caller's id and
     email already come from the token. Customer name: decide in Step 6 (token claim or call).

2. `bookings.show_id` -> `shows`, and show -> movie name in BookingMapper
   - Today: `booking.getShow().getMovie().getName()`.
   - Replace with: keep `show_id` as a plain number. Save movie name and show time on the
     booking row when it is created (a snapshot). History stays correct even if the movie is renamed.

3. `booking_seats.seat_id` -> `seats` (ManyToMany between Booking and Seat)
   - Replace with: `booking_seats` stores `seat_id` and `seat_number` as plain columns, no FK.

4. Seat status changed inside the booking transaction (BIGGEST ONE)
   - Today: one transaction marks seats BOOKED and saves the booking. Cancel does the reverse.
   - Replace with: Catalog exposes "reserve seats" and "release seats" calls. Catalog does the
     DB row lock, the status change and the price lookup in its own local transaction.
   - Price: Catalog returns the seat numbers and prices, Booking sums the total.
   - Cost: two services, two databases, no shared transaction. If Catalog says OK and Booking
     then fails to save, seats stay BOOKED with no booking. The Phase 2 half-saved bug returns
     on purpose. Phase 8 (saga) fixes it.

5. Seat hold checks
   - Today: BookingFacade calls `SeatHoldService.assertHeldByUser`, which reads Redis.
     SeatHoldService also reads the `seats` table.
   - Replace with: holds live in Catalog, next to the seats. The reserve call carries the
     user id and Catalog checks the hold itself.

6. Security code shared by every service
   - Today: JwtAuthenticationFilter, AuthUser, 401/403 handlers, ErrorResponse.
   - Replace with: a small `common` module (Step 2). Every service verifies the token with the
     same secret until the Gateway takes over in Step 8.

### Already clean (no work needed)
- `booking_attempt_log.show_id` is a plain number with no FK.
- The JWT carries uid, email and role, so identity needs no user lookup.

### Decisions and alternatives rejected
- Seat status lives in Catalog, not Booking.
  - Why: the Phase 4 lock (`FOR UPDATE` + `@Version`) stays inside one database transaction.
  - Rejected: Booking owns seat status. The lock would have to move, and the seat chart would
    need booking data.
- Holds live in Catalog, not Booking.
  - Why: a hold only makes sense next to the seat it protects.
  - Rejected: holds in Booking. Booking would need to ask Catalog if each seat exists.
- No separate Inventory service.
  - Rejected: five services are enough to learn from, a sixth only adds more moving parts.
- Payment and Notification stay new and simple. The code has none today.