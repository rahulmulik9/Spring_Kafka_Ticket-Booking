# Phase 8: Booking Flow with Kafka Events

State after Step 4. Services talk only through Kafka events. Nobody waits for anybody.

---

## 1. Small flow (read this first)

```
Customer --> Booking: POST /api/bookings/{showId}   (answer: 202, status PENDING)

Booking  --booking-created-->        Cinema     (reserve the seats)

Cinema   --seats-reserved-->         Payment    (charge the money)
                                     Booking    (fill in movie, seats, total)
Cinema   --seats-reservation-failed--> Booking  (a seat is taken)

Payment  --payment-completed-->      Booking    (mark CONFIRMED)
Payment  --payment-failed-->         Booking    (mark PAYMENT_FAILED)

Booking  --booking-confirmed-->      Notification
Booking  --booking-failed-->         Cinema (release seats) and Notification
```

**Topics and who publishes them**

- `booking-created`: Booking
- `seats-reserved`: Cinema
- `seats-reservation-failed`: Cinema
- `payment-completed`: Payment
- `payment-failed`: Payment
- `booking-confirmed`: Booking
- `booking-failed`: Booking
- `booking-cancelled`: Booking (cancel flow, not drawn here)

**Final booking status in each scenario**

- Scenario 1, booking completed: `CONFIRMED`
- Scenario 2, seat not available: `SEATS_UNAVAILABLE`
- Scenario 3, payment failed: `PAYMENT_FAILED`

**Rules to remember for the next steps**

- The `POST` returns before anything else happens. The client checks the status with `GET /api/bookings/{id}`.
- Every arrow below is "save to database, then publish to Kafka". These are two separate actions. That gap is the subject of Step 5 (message loss) and Step 6 (outbox).
- Order across different topics is not guaranteed. `seats-reserved` has two listeners (Booking and Payment), and either can finish first.
- Each service has its own copy of every event class. The JSON is the contract.

---

## 2. Scenario 1: Booking completed

The happy path. Seats are free and the amount is within the limit of 1000.

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant B as Booking Service
    participant K as Kafka
    participant CI as Cinema Service
    participant P as Payment Service
    participant N as Notification Service

    C->>B: POST /api/bookings/{showId} (BookingController.createBooking)
    B->>B: BookingFacade.createBooking then doCreateBooking
    B->>B: BookingService.createPendingBooking (status PENDING)
    B->>K: booking-created (BookingEventPublisher.publishBookingCreated)
    B->>B: AuditService.logAttempt (REQUIRES_NEW)
    B-->>C: 202 Accepted, status PENDING

    K->>CI: booking-created (BookingCreatedListener.onBookingCreated)
    CI->>CI: SeatReservationService.reserveSeats (lock seats, mark BOOKED)
    CI->>K: seats-reserved (CinemaEventPublisher.publishSeatsReserved)

    par Booking fills in the details
        K->>B: seats-reserved (SeatsReservedListener.onSeatsReserved)
        B->>B: BookingService.addReservationDetails (movie, show time, total, seats)
    and Payment charges
        K->>P: seats-reserved (BookingEventListener.onSeatsReserved)
        P->>P: PaymentService.pay (status SUCCESS)
        P->>K: payment-completed (PaymentEventPublisher.publishPaymentCompleted)
    end

    K->>B: payment-completed (PaymentCompletedListener.onPaymentCompleted)
    B->>B: BookingService.updateStatus (CONFIRMED)
    B->>K: booking-confirmed (BookingEventPublisher.publishBookingConfirmed)

    K->>N: booking-confirmed (BookingConfirmedListener.onBookingConfirmed)
    N->>N: Log "Sending confirmation to ..."

    C->>B: GET /api/bookings/{id}
    B-->>C: status CONFIRMED, movie, seats and total filled in
```

**End state**

- Booking: `CONFIRMED`, with movie name, show time, total and seat numbers
- Cinema: seats are `BOOKED`
- Payment: one row with status `SUCCESS`

**Note:** the two `seats-reserved` listeners are independent. They update different columns of the booking, which is why `Booking` has `@DynamicUpdate`.

---

## 3. Scenario 2: Booking failed, seat not available

A requested seat is already `BOOKED`. Cinema refuses, and no payment is attempted.

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant B as Booking Service
    participant K as Kafka
    participant CI as Cinema Service
    participant N as Notification Service

    C->>B: POST /api/bookings/{showId} (BookingController.createBooking)
    B->>B: BookingService.createPendingBooking (status PENDING)
    B->>K: booking-created (BookingEventPublisher.publishBookingCreated)
    B-->>C: 202 Accepted, status PENDING

    K->>CI: booking-created (BookingCreatedListener.onBookingCreated)
    CI->>CI: SeatReservationService.reserveSeats throws SeatAlreadyBookedException
    Note over CI: Nothing was changed, so there is nothing to undo
    CI->>K: seats-reservation-failed (CinemaEventPublisher.publishSeatsReservationFailed)

    K->>B: seats-reservation-failed (SeatsReservationFailedListener.onSeatsReservationFailed)
    B->>B: BookingService.updateStatus (SEATS_UNAVAILABLE)
    B->>K: booking-failed with empty seatIds (BookingEventPublisher.publishBookingFailed)

    par Cinema receives it
        K->>CI: booking-failed (SeatReleaseListener.onBookingFailed)
        CI->>CI: seatIds is empty, so nothing to release
    and Notification receives it
        K->>N: booking-failed (BookingFailedListener.onBookingFailed)
        N->>N: Log "Sending failure message ... Seat A1 is already booked"
    end

    C->>B: GET /api/bookings/{id}
    B-->>C: status SEATS_UNAVAILABLE (movie, total and seats are null)
```

**End state**

- Booking: `SEATS_UNAVAILABLE`, with `movieName`, `showTime`, `totalAmount` null and no seats. This is expected, because `seats-reserved` was never sent.
- Cinema: seats unchanged
- Payment: no row for this booking

**Note:** this is a business refusal, not a crash. Cinema answers with an event instead of throwing, so Kafka does not retry it.

---

## 4. Scenario 3: Booking failed, payment failed

Seats were reserved, but the amount is over the limit of 1000 (for example 5 seats at 250). The saga must undo the seat reservation.

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant B as Booking Service
    participant K as Kafka
    participant CI as Cinema Service
    participant P as Payment Service
    participant N as Notification Service

    C->>B: POST /api/bookings/{showId} (BookingController.createBooking)
    B->>B: BookingService.createPendingBooking (status PENDING)
    B->>K: booking-created (BookingEventPublisher.publishBookingCreated)
    B-->>C: 202 Accepted, status PENDING

    K->>CI: booking-created (BookingCreatedListener.onBookingCreated)
    CI->>CI: SeatReservationService.reserveSeats (seats become BOOKED)
    CI->>K: seats-reserved (CinemaEventPublisher.publishSeatsReserved)

    par Booking fills in the details
        K->>B: seats-reserved (SeatsReservedListener.onSeatsReserved)
        B->>B: BookingService.addReservationDetails
    and Payment tries to charge
        K->>P: seats-reserved (BookingEventListener.onSeatsReserved)
        P->>P: PaymentService.pay (amount over limit, status FAILED, row is kept)
        P->>K: payment-failed with showId and seatIds (PaymentEventPublisher.publishPaymentFailed)
    end

    K->>B: payment-failed (PaymentFailedListener.onPaymentFailed)
    B->>B: BookingService.updateStatus (PAYMENT_FAILED)
    B->>K: booking-failed with seatIds (BookingEventPublisher.publishBookingFailed)

    par Compensating action
        K->>CI: booking-failed (SeatReleaseListener.onBookingFailed)
        CI->>CI: SeatReservationService.releaseSeats (seats become AVAILABLE)
    and Tell the customer
        K->>N: booking-failed (BookingFailedListener.onBookingFailed)
        N->>N: Log "Sending failure message ... Amount exceeds the allowed limit of 1000"
    end

    C->>B: GET /api/bookings/{id}
    B-->>C: status PAYMENT_FAILED
```

**End state**

- Booking: `PAYMENT_FAILED`, with movie, total and seat numbers filled in (the seats were reserved before the payment failed)
- Cinema: seats are `AVAILABLE` again
- Payment: one row with status `FAILED` and a failure reason

**Notes**

- The compensating action is `SeatReservationService.releaseSeats`, triggered by `booking-failed`. It is safe to run twice.
- `payment-failed` and `booking-failed` both carry `seatIds`. Payment may answer before Booking has stored the seats, so the release must not depend on Booking's own copy.
- `PaymentService.pay` returns the `FAILED` payment instead of throwing. Throwing inside `@Transactional` would roll back the `FAILED` row.
- `PaymentFailed` is a fact from Payment. `BookingFailed` is Booking's own decision, and it is what Cinema and Notification react to.

---

## 5. Class map

**Booking** (`com.rahul.bookingservice`)

- `controller.BookingController`
- `service.BookingFacade`, `service.BookingService`, `service.AuditService`
- `kafka.publisher.BookingEventPublisher`
- `kafka.listener.SeatsReservedListener`, `SeatsReservationFailedListener`, `PaymentCompletedListener`, `PaymentFailedListener`

**Cinema** (`com.rahul.cinemaservice`)

- `service.SeatReservationService`
- `kafka.publisher.CinemaEventPublisher`
- `kafka.listener.BookingCreatedListener`, `SeatReleaseListener`

**Payment** (`com.rahul.paymentservice`)

- `service.PaymentService`
- `kafka.publisher.PaymentEventPublisher`
- `kafka.listener.BookingEventListener` (listens to `seats-reserved`)

**Notification** (`com.rahul.notificationservice`)

- `kafka.listener.BookingConfirmedListener`, `BookingFailedListener`

---

## 6. Gaps the next steps will close

Each `save, then publish` in these diagrams can lose the message if the service crashes between the two.

- **Step 5:** reproduce the loss. Crash Booking right after `createPendingBooking`, and the booking stays `PENDING` forever.
- **Step 6:** the outbox pattern fixes it.
- **Step 7:** idempotency keys stop a double click from creating two bookings.
- **Step 8:** idempotent consumers make duplicate messages harmless.
- **Step 9:** retries and a dead letter queue handle messages that keep failing.
