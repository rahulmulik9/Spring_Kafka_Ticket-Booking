# Booking Flow

How a customer books seats, from the first request to the notification.

Services involved:

- **API Gateway:** the single front door
- **Booking Service:** runs the booking from start to end
- **Cinema Service:** owns the movies, shows and seats
- **Payment Service:** takes the payment
- **Notification Service:** tells the customer what happened

---

## 1. Simple flow: how a booking is done

**Before the booking:** the customer logs in with the User Service and gets an access token. The token is a signed pass that carries the user id, the email and the role. The customer sends it with every protected request.

1. **The customer sends a booking request.** The request says which show and which seats, and carries the token. It goes to the Gateway, never directly to a service.

2. **The Gateway checks the token.** `JwtGatewayFilter` makes sure the token is genuine and not expired. A missing or fake token is turned away here with `401`, and no service ever sees it. If the token is fine, the Gateway looks up Booking Service in Eureka and forwards the request.

3. **Booking Service finds out who is calling.** `JwtAuthenticationFilter` checks the token again, because every service verifies for itself. It builds the caller (`AuthUser`: id, email, role). Then `BookingController.createBooking` hands the request to `BookingFacade.createBooking`.

4. **Booking Service asks Cinema Service to reserve the seats.** `CinemaClient.reserveSeats` makes the call, and `FeignConfig` copies the customer's token onto it, so Cinema Service knows who is behind the request. Cinema Service runs `SeatReservationService.reserveSeats`, which:
   - locks the requested seats so nobody else can touch them at the same moment,
   - checks that every seat exists, belongs to this show and is still free,
   - marks all of them as booked, or none of them if even one is taken,
   - sends back the movie name, the show time, and each seat's number and price.

5. **Booking Service saves the booking as PENDING.** `BookingService.createPendingBooking` copies the movie name, show time, seat numbers and prices into its own database, and adds up the total. It saves the booking before payment because the payment needs a booking number to be charged against.

6. **Booking Service asks Payment Service to charge the total.** `PaymentClient.pay` makes the call. `PaymentService.pay` first checks that this booking is not already paid. It then checks the amount against the limit. Within the limit, the payment is recorded as SUCCESS. Over the limit, it is recorded as FAILED with a reason. The answer goes back to Booking Service either way.

7. **If the payment succeeded, the booking is confirmed.** `BookingService.updateStatus` changes the booking from PENDING to CONFIRMED.

8. **Booking Service tells Notification Service.** `NotificationClient.send` makes the call. `NotificationService.send` writes the message ("Your booking is confirmed") and hands it to `LogNotificationSender.send`, which logs it. If sending fails for any reason, Booking Service ignores it, because a failed message must never undo a confirmed booking.

9. **The attempt is recorded.** `AuditService.logAttempt` writes a row for this attempt, successful or not. It uses its own separate transaction so the record stays even when the booking fails.

10. **The customer gets the answer.** `BookingMapper.toResponse` builds the response with the booking number, movie, show time, seat numbers, total and status. It returns through the Gateway as `201 Created`.

**Two rules the flow follows:**

- No database transaction stays open while another service is being called. Each save is its own short step.
- The data copied into the booking (movie name, seats, prices) means Booking Service never has to ask Cinema Service again just to show a booking.

---

## 2. What happens when something goes wrong

- **A seat is already taken.** Cinema Service refuses the whole request and changes nothing. Booking Service saves nothing and the customer gets `409`.
- **Cinema Service is down.** Nothing has been done yet, so nothing needs undoing. The customer gets `503` or `502`.
- **The booking cannot be saved after the seats were reserved.** Booking Service gives the seats back with `CinemaClient.releaseSeats`, which runs `SeatReservationService.releaseSeats`, and then reports the error.
- **The payment is declined (amount over the limit).** Booking Service releases the seats and marks the booking PAYMENT_FAILED, so a record of the attempt remains. It sends a "payment failed" notification, and the customer gets `402`.
- **Payment Service is down or unreachable.** The same clean-up as a declined payment. The customer gets `503` or `502`.
- **Notification Service is down.** Nothing changes for the customer. The booking stays confirmed, and Booking Service writes a warning in its log.

---

## 3. Sequence diagrams

### 3.1 Successful booking

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant G as API Gateway
    participant B as Booking Service
    participant CI as Cinema Service
    participant P as Payment Service
    participant N as Notification Service

    C->>G: Book seats of a show (token and seat ids)
    G->>G: Check the token is genuine (JwtGatewayFilter)
    G->>B: Forward the request (service found through Eureka)
    B->>B: Read the token, find the caller (JwtAuthenticationFilter)
    B->>B: BookingController.createBooking then BookingFacade.createBooking

    B->>CI: Reserve the seats (CinemaClient.reserveSeats)
    CI->>CI: Lock seats, check all are free, mark BOOKED (SeatReservationService.reserveSeats)
    CI-->>B: Movie name, show time, seat numbers and prices

    B->>B: Save the booking as PENDING (BookingService.createPendingBooking)

    B->>P: Charge the total (PaymentClient.pay)
    P->>P: Check already paid and limit, save the payment (PaymentService.pay)
    P-->>B: SUCCESS

    B->>B: Mark the booking CONFIRMED (BookingService.updateStatus)

    B->>N: Send the confirmation (NotificationClient.send)
    N->>N: Build the message and log it (NotificationService.send)
    N-->>B: Accepted

    B->>B: Record the attempt (AuditService.logAttempt)
    B-->>G: Booking details
    G-->>C: 201 Created
```

### 3.2 Payment declined

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant G as API Gateway
    participant B as Booking Service
    participant CI as Cinema Service
    participant P as Payment Service
    participant N as Notification Service

    C->>G: Book 5 seats (token and seat ids)
    G->>G: Check the token is genuine
    G->>B: Forward the request
    B->>B: BookingFacade.createBooking

    B->>CI: Reserve the seats (CinemaClient.reserveSeats)
    CI-->>B: Seat details, seats are now BOOKED

    B->>B: Save the booking as PENDING

    B->>P: Charge the total (PaymentClient.pay)
    P->>P: Amount is over the limit, save the payment as FAILED
    P-->>B: FAILED with a reason

    B->>CI: Give the seats back (CinemaClient.releaseSeats)
    CI->>CI: Mark the seats AVAILABLE again (SeatReservationService.releaseSeats)
    CI-->>B: Done

    B->>B: Mark the booking PAYMENT_FAILED (BookingService.updateStatus)
    B->>N: Send the payment failed message (NotificationClient.send)
    N-->>B: Accepted

    B->>B: Record the failed attempt (AuditService.logAttempt)
    B-->>G: Error PAYMENT_FAILED
    G-->>C: 402 Payment Required
```

### 3.3 Seat already taken

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant G as API Gateway
    participant B as Booking Service
    participant CI as Cinema Service

    C->>G: Book seats that someone else already booked
    G->>B: Forward the request (token checked)
    B->>CI: Reserve the seats (CinemaClient.reserveSeats)
    CI->>CI: Lock seats, one is BOOKED, change nothing (SeatReservationService.reserveSeats)
    CI-->>B: Error SEAT_ALREADY_BOOKED
    B->>B: Turn it back into an exception (FeignErrorDecoder)
    B->>B: Record the failed attempt (AuditService.logAttempt)
    B-->>G: Error SEAT_ALREADY_BOOKED
    G-->>C: 409 Conflict
```
