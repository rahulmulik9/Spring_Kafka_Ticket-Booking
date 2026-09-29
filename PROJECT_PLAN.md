# Project Plan: Ticket Booking System

Add this file to the project context together with `TEACHING_METHOD.md`.

- `TEACHING_METHOD.md` says **how** each step is taught.
- `PROJECT_PLAN.md` (this file) says **what** we build, phase by phase, and where we are.

---

## Current position

**Phase 2, Step 1: Reproduce the half-saved bug** (update this line as you progress, and paste it at the start of each session)

---

## 1. Project overview

A Ticket Booking System (like BookMyShow), built incrementally in 11 phases. Each phase starts from a problem in the previous version and adds one technology to fix it. Running example: a cinema counter that sells seats.

**Learner goal:** prepare for Java and Spring Boot interviews (3.5 years of experience) with one strong project, and share the learning on LinkedIn.

## 2. Ground rules

- **One step at a time.** Never move on until the current step works and is committed.
- **Measure** before and after in Phases 3, 4, and 6.
- **Git:** commit after every step, and tag at the end of each phase (`phase-N-name`).
- **Notes:** a short `NOTES.md` per phase with problem, fix, alternative rejected, and numbers.
- **Tests:** write basic unit tests from Phase 1. Phase 11 is the deep version.
- **Each step in the plan below is an overview** (Why, How, What). The full version with code and testing is written only when the learner says "Start Phase X, Step Y".


# Phases

## Phase 1: Basic CRUD

- **Lacking:** nothing yet, this is the starting point.
- **What we do:** a simple working monolith with Spring Boot, JPA, PostgreSQL in Docker, and Flyway.
- **What we achieve:** a user can view movies, see seats, and book them.
- **Concepts to know first:**
  - Spring Boot basics: auto-configuration, starters, `application.yml`
  - Layered architecture: controller, service, repository
  - JPA basics: `@Entity`, `@OneToMany`, `@ManyToOne`, default fetch types
  - Spring Data JPA: `JpaRepository`, derived query methods
  - REST basics: HTTP methods, status codes
  - Docker and docker-compose basics
  - Flyway and why not `ddl-auto`
  - SOLID: single responsibility, dependency inversion

**Steps**

- **Step 1: Project setup**
  - Why: we need a working foundation, like a shop with lights and electricity before goods arrive.
  - How: create the Spring Boot project, run the database in Docker, and connect the two.
  - What: an app that starts and talks to its database.
- **Step 2: Database structure with Flyway**
  - Why: the way data is stored should be tracked like code, like a numbered blueprint with a change log.
  - How: write versioned migration files that create the storage areas and add sample movies.
  - What: the database builds itself on startup, with sample data ready.
- **Step 3: Entities and repositories**
  - Why: the app must understand the real-world things it manages: movies, shows, seats, and bookings.
  - How: map each thing to a Java class, and give each a repository that saves and finds it.
  - What: the app can read and write all four things.
- **Step 4: Movie features**
  - Why: nothing can be booked until movies exist. This is the movie list on the cinema wall.
  - How: add create, list, and view-one for movies.
  - What: movies can be added and browsed.
- **Step 5: Show features**
  - Why: a movie needs timings. "Avengers" has a 6 PM show and a 9 PM show, each with its own seats.
  - How: add show creation and generate seats automatically (A1, A2, and so on).
  - What: every show comes with a full set of seats.
- **Step 6: Seat viewing**
  - Why: users need to see which seats are free before choosing, like a seating chart.
  - How: list the seats of a show with their status.
  - What: users can see available and booked seats.
- **Step 7: Booking**
  - Why: this is the core purpose of the app.
  - How: check the chosen seats are free, mark them booked, calculate the price, and save the booking.
  - What: a user can book seats and view the booking.
- **Step 8: Cancel booking**
  - Why: people change plans, and cancelled seats should go back on sale.
  - How: mark the booking cancelled and free its seats.
  - What: cancelling releases the seats.
- **Step 9: Basic tests and tag**
  - Why: we need proof the basics work before building on them.
  - How: write a few simple tests, try everything from Postman, and tag `phase-1-basic`.
  - What: a stable first version saved as a checkpoint.

**Done when:** you can create a movie and a show, see its seats, book them, and cancel to free them. Tag `phase-1-basic`.

**Interview line:** "I started with a simple monolith so the business flow worked end to end before adding complexity."

**LinkedIn posts**
- Decision: why I used Flyway instead of `ddl-auto=update`
- Concept: why I turned off open-in-view
- Milestone: Phase 1 done, with entity diagram and GitHub link

---

## Phase 2: Transactions and Error Handling

- **Lacking:** if seats are marked booked but the booking fails to save, data is left half-saved. Errors show raw stack traces.
- **What we do:** `@Transactional` with rollback rules, custom exceptions, a global exception handler, DTOs with validation, and profiles.
- **What we achieve:** data stays consistent even when something fails, and clients get clean error messages.
- **Concepts to know first:**
  - ACID properties
  - How `@Transactional` works: proxies, JDK vs CGLIB
  - Rollback rules: checked vs unchecked exceptions
  - Propagation types
  - Self-invocation problem
  - `@RestControllerAdvice` and `@ExceptionHandler`
  - DTO vs entity, Bean Validation
  - Spring profiles

**Steps**

- **Step 1: Reproduce the half-saved bug**
  - Why: we should feel the problem before fixing it. The seat is marked sold, but the ticket is never issued.
  - How: force a failure in the middle of booking and check the data.
  - What: proof that the app can leave broken, half-finished data.
- **Step 2: Add transactions**
  - Why: a booking should be all-or-nothing, like a bank transfer where money never leaves one account without reaching the other.
  - How: wrap the booking in a transaction and repeat the failure.
  - What: a failed booking leaves no trace.
- **Step 3: Self-invocation pitfall**
  - Why: a transaction silently stops working when a method calls another method in the same class. It is a classic interview trap.
  - How: reproduce it, understand the proxy behind it, and fix it by moving the method to another class.
  - What: transactions work reliably.
- **Step 4: Rollback rules and propagation**
  - Why: not every error cancels the work by default, and sometimes one operation must run inside or apart from another.
  - How: experiment with rollback for checked and unchecked errors, and with the propagation types.
  - What: control over exactly when work is undone.
- **Step 5: Custom exceptions**
  - Why: "Seat already taken" is more useful than a generic error, for users and developers.
  - How: create named exceptions for the common failures.
  - What: every failure has a clear meaning.
- **Step 6: Global error handling**
  - Why: users should never see technical stack traces.
  - How: catch all exceptions in one place and return one consistent message format.
  - What: clean, uniform error responses.
- **Step 7: DTOs and validation**
  - Why: what the outside world sends and receives should differ from what is stored. A customer needs a ticket, not our internal records.
  - How: create request and response objects, and validate input like missing names or invalid emails.
  - What: safe, predictable input and output.
- **Step 8: Profiles and configuration**
  - Why: development and live systems need different settings.
  - How: create `dev` and `prod` profiles and move settings out of the code.
  - What: the same app runs in different environments.
- **Step 9: Transaction tests and tag**
  - Why: the all-or-nothing guarantee must be proven.
  - How: write tests that force failures and check nothing is left behind, then tag `phase-2-transactions`.
  - What: a checkpoint with a proven consistency guarantee.

**Done when:** a forced failure leaves no partial data, and every error returns a clean response. Tag `phase-2-transactions`.

**Interview line:** "Booking touched multiple tables, so I made it atomic. I also hit the self-invocation pitfall with `@Transactional` and fixed it."

**LinkedIn posts**
- Bug story: seat marked booked, but the ticket was never created
- Mistake: I added `@Transactional` and nothing rolled back
- Concept: checked vs unchecked exceptions and rollback, with a bank transfer example

---

## Phase 3: Database Performance

- **Lacking:** movie listing fires one query per movie (N+1), search is slow, and lists have no pagination.
- **What we do:** seed 100k+ records, reproduce N+1, fix it, add DTO projections, pagination, indexes, and pool tuning.
- **What we achieve:** fast listing and search, with before and after numbers.
- **Concepts to know first:**
  - Lazy vs eager loading, `LazyInitializationException`
  - N+1 problem and how it occurs
  - Fetch join, `@EntityGraph`, DTO projections
  - Why fetch join with pagination gives a warning
  - Indexes: B-tree, composite, and when an index is not used
  - `EXPLAIN` and `EXPLAIN ANALYZE`
  - Pagination: `Pageable`, offset vs keyset
  - Connection pooling and HikariCP

**Steps**

- **Step 1: Create large data**
  - Why: everything is fast with 10 rows, and real problems show only at scale.
  - How: run a script that inserts over 100,000 records.
  - What: a realistic database to test against.
- **Step 2: Reproduce the N+1 problem**
  - Why: the app may ask the database one question for the list and one more per item, like calling a shop 100 times to ask about 100 products.
  - How: turn on SQL logging and count the queries.
  - What: visible proof of the extra queries.
- **Step 3: Record baseline numbers**
  - Why: without "before" numbers we cannot prove an improvement.
  - How: note the response time and query count of the key screens.
  - What: a baseline table.
- **Step 4: Fix N+1**
  - Why: fewer trips to the database means faster responses.
  - How: use a fetch join, then try `@EntityGraph` and compare.
  - What: one query replaces hundreds.
- **Step 5: Lighter read-only results**
  - Why: a listing does not need every detail, like a menu that shows names and prices only.
  - How: use DTO projections that fetch only the needed fields.
  - What: smaller, faster listings.
- **Step 6: Pagination and sorting**
  - Why: sending 100,000 movies at once is wasteful, so we show them page by page.
  - How: use `Pageable` for page number, size, and sort order.
  - What: listings that return quickly in small portions.
- **Step 7: Indexes**
  - Why: an index works like a book's index: you jump to the page instead of reading everything.
  - How: run `EXPLAIN ANALYZE` on slow searches and add indexes where the database scans everything.
  - What: fast search on large data.
- **Step 8: Connection pool tuning**
  - Why: opening database connections is costly. Too few connections make users wait, and too many overload the database.
  - How: adjust HikariCP settings and observe the effect under load.
  - What: a pool sized sensibly.
- **Step 9: Comparison and tag**
  - Why: numbers make the story credible in interviews.
  - How: build a before and after table, then tag `phase-3-performance`.
  - What: measurable proof of improvement.

**Done when:** you have a before and after table of query counts and response times. Tag `phase-3-performance`.

**Interview line:** "With 100k rows the listing took X ms. After fixing N+1 and adding an index it took Y ms."

**LinkedIn posts**
- Concept: N+1 explained with a shop example
- Before and after numbers: movie listing X ms to Y ms
- Mistake: I added an index and the database still ignored it
- Milestone: Phase 3 done, with the comparison table

---

## Phase 4: Concurrency and Locking

- **Lacking:** two users can book the same seat at the same moment, and transactions alone do not prevent it.
- **What we do:** a multithreaded test that breaks it, then optimistic and pessimistic locking, and a comparison.
- **What we achieve:** no double booking, even under heavy load.
- **Concepts to know first:**
  - Threads, `ExecutorService`, `CountDownLatch`
  - Race conditions and lost updates
  - Isolation levels and anomalies (dirty read, non-repeatable read, phantom read)
  - Optimistic locking and how `@Version` works
  - Pessimistic locking and `SELECT ... FOR UPDATE`
  - Deadlocks and how to avoid them
  - State pattern for seat status

**Steps**

- **Step 1: Multithreaded test**
  - Why: two people clicking on the same seat at the same second is a real situation.
  - How: send 100 simultaneous booking requests for one seat using `ExecutorService` and `CountDownLatch`.
  - What: a repeatable test that simulates a crowd.
- **Step 2: Observe the failure**
  - Why: we need to see the double booking to understand why transactions alone are not enough.
  - How: run the test, count the successful bookings, and study isolation levels.
  - What: proof that several people can book one seat.
- **Step 3: Optimistic locking**
  - Why: assume clashes are rare and detect them at save time, like two people editing a document where the second save is rejected.
  - How: add a version field to the seat.
  - What: only one booking succeeds per seat.
- **Step 4: Handle the conflict**
  - Why: the rejected user should get a friendly message or an automatic retry.
  - How: catch the locking exception and retry or respond clearly.
  - What: conflicts handled gracefully.
- **Step 5: Pessimistic locking**
  - Why: assume clashes are common and lock the seat while one person books, like a fitting room with a lock.
  - How: use `SELECT ... FOR UPDATE` on the seat.
  - What: others wait their turn, so double booking is impossible.
- **Step 6: Timeouts and deadlocks**
  - Why: locks can cause waiting forever, or two requests waiting on each other.
  - How: set lock timeouts and always lock seats in the same order.
  - What: locking that cannot freeze the system.
- **Step 7: Compare both**
  - Why: knowing when to use each is a favorite interview question.
  - How: run both under light and heavy competition and compare speed and failures.
  - What: a clear comparison.
- **Step 8: Document and tag**
  - Why: decisions should be recorded while they are fresh.
  - How: write down when to pick which lock, then tag `phase-4-locking`.
  - What: a checkpoint with a documented decision.

**Done when:** the test passes with exactly one successful booking, and you have notes on when to use which lock. Tag `phase-4-locking`.

**Interview line:** "I reproduced double booking with 100 threads, then chose optimistic locking because conflicts were rare, and here is when I would pick pessimistic instead."

**LinkedIn posts**
- Bug story: 100 threads booked the same seat
- Decision: optimistic vs pessimistic locking
- Mistake: my pessimistic lock caused a deadlock
- Milestone: Phase 4 done, with the test result screenshot

---

## Phase 5: Security (JWT and Roles)

- **Lacking:** anyone can call any API, and there is no difference between user, organizer, and admin.
- **What we do:** Spring Security, JWT, roles, method-level security, and ownership of bookings.
- **What we achieve:** only the right person can do the right action.
- **Concepts to know first:**
  - Authentication vs authorization
  - Spring Security filter chain and `SecurityContext`
  - Password hashing with BCrypt
  - JWT structure: header, payload, signature, expiry
  - JWT vs session, and the revocation problem
  - Access token vs refresh token
  - Role-based access and `@PreAuthorize`
  - Chain of Responsibility

**Steps**

- **Step 1: Users, roles, and passwords**
  - Why: the app needs to know who is using it, like a membership card at the counter.
  - How: create users with roles (USER, ORGANIZER, ADMIN) and store passwords hashed with BCrypt.
  - What: users exist and their passwords are safe even if the database leaks.
- **Step 2: Registration**
  - Why: new people must be able to join.
  - How: accept details, check for duplicates, and save the user.
  - What: sign-up works.
- **Step 3: Login with JWT**
  - Why: after login the user gets a signed pass to show on every visit, so they do not enter the password each time.
  - How: verify credentials and return a signed JWT with an expiry.
  - What: login returns a token.
- **Step 4: JWT filter**
  - Why: every request should be checked at the door before it reaches the app.
  - How: add a filter in the Spring Security chain that reads and verifies the token.
  - What: only requests with a valid token get in.
- **Step 5: Access rules by URL**
  - Why: some areas are public, like browsing movies, while others are private.
  - How: define which paths need which roles.
  - What: broad protection in place.
- **Step 6: Access rules on methods**
  - Why: URL rules alone are coarse, so we also protect the business actions themselves.
  - How: use `@PreAuthorize` on service methods.
  - What: fine-grained permissions.
- **Step 7: Bookings belong to users**
  - Why: one customer must never see another's bookings.
  - How: link each booking to the logged-in user and check ownership on every read.
  - What: private data stays private.
- **Step 8: Refresh token and logout**
  - Why: short-lived tokens are safer, but users should not have to log in again every few minutes.
  - How: issue a refresh token and handle logout by invalidating it.
  - What: safe, smooth sessions.
- **Step 9: Security tests and tag**
  - Why: security should be proven, not assumed.
  - How: test that each role can do only its own actions, then tag `phase-5-security`.
  - What: a checkpoint with verified access control.

**Done when:** each role can do only its own actions, and a user cannot read another user's booking. Tag `phase-5-security`.

**Interview line:** "Users book, organizers create movies, admins manage everything. Authorization is enforced at the method level, not just the URL level."

**LinkedIn posts**
- Concept: how I store passwords and what happens if the database leaks
- Concept: the JWT logout problem
- Mistake: a user could read another user's booking

---

## Phase 6: Redis (Caching and Seat Hold)

- **Lacking:** every catalog read hits the database, and a user picking a seat cannot hold it briefly while paying.
- **What we do:** Redis caching with invalidation, a seat hold with expiry, a distributed lock, and login rate limiting.
- **What we achieve:** faster reads and automatic release of abandoned seats.
- **Concepts to know first:**
  - Redis data structures: string, hash, list, set, sorted set
  - TTL and eviction policies
  - Caching strategies: cache-aside, write-through, write-behind
  - `@Cacheable`, `@CacheEvict`, `@CachePut`
  - Cache problems: penetration, stampede, avalanche
  - Distributed locks and their risks
  - Rate limiting algorithms: fixed window, sliding window, token bucket
  - Serialization in Redis

**Steps**

- **Step 1: Add Redis**
  - Why: we need a very fast memory store next to the database.
  - How: add Redis to docker-compose and connect the app.
  - What: Redis running and reachable.
- **Step 2: Cache the catalog**
  - Why: the movie list rarely changes but is read constantly. It is like keeping today's menu on the counter instead of walking to the kitchen each time.
  - How: cache reads with an expiry time.
  - What: repeated reads come from memory.
- **Step 3: Cache invalidation**
  - Why: a cache that shows old data is worse than no cache.
  - How: evict or update the cache when movies change.
  - What: the cache stays correct.
- **Step 4: Cache problems**
  - Why: caches fail in known ways, such as stale data and a stampede when thousands hit the database the moment an entry expires.
  - How: reproduce each problem and apply fixes like random expiry and locking.
  - What: a cache that holds up under pressure.
- **Step 5: Seat hold**
  - Why: a user choosing a seat needs a few minutes to pay, and others should not grab it meanwhile.
  - How: store a temporary hold in Redis that expires after 5 minutes.
  - What: seats are held, then released automatically.
- **Step 6: Connect the hold to booking**
  - Why: the hold is only useful if booking respects it.
  - How: the flow becomes hold, pay, then confirm or expire.
  - What: a realistic booking journey.
- **Step 7: Distributed lock**
  - Why: with several app copies, we should compare a Redis-based lock with database locks.
  - How: use Redisson and compare it with the Phase 4 locks.
  - What: an understanding of locking across many servers.
- **Step 8: Rate limiting on login**
  - Why: it stops someone from trying thousands of passwords.
  - How: count attempts per user in Redis and block after a limit.
  - What: login protected from abuse.
- **Step 9: Measure and tag**
  - Why: prove the gain.
  - How: compare response times with and without the cache, then tag `phase-6-redis`.
  - What: a checkpoint with measured results.

**Done when:** repeated reads are served from cache, and an unpaid hold expires by itself. Tag `phase-6-redis`.

**Interview line:** "Catalog reads are heavy and rarely change, so I cached them. The seat hold uses a TTL so abandoned carts release automatically."

**LinkedIn posts**
- Before and after numbers: catalog reads with and without cache
- Concept: cache stampede in plain words
- Bug story: users saw old movie data because the cache was not invalidated
- Decision: Redis seat hold vs holding the seat in the database

---

## Phase 7: Microservices Split

- **Lacking:** one codebase means payment load cannot scale separately from browsing, and one bug can take everything down.
- **What we do:** split into User, Catalog, Booking, Payment, and Notification services, with Gateway, Eureka, Feign, and Config Server.
- **What we achieve:** independent services that can be deployed and scaled separately.
- **Concepts to know first:**
  - Monolith vs microservices trade-offs
  - Domain-driven design basics: bounded context
  - Database per service, and what you lose (joins, transactions)
  - Service discovery and client-side load balancing
  - API Gateway responsibilities
  - OpenFeign and REST communication
  - Centralized configuration
  - Stateless services and horizontal scaling

**Steps**

- **Step 1: Define boundaries**
  - Why: splitting badly creates a mess. Each service should own one business area, like separate counters for tickets, payments, and announcements.
  - How: list the areas and decide who owns which data.
  - What: a clear service map.
- **Step 2: Multi-module project and Config Server**
  - Why: many services need one place to keep settings.
  - How: set up the project structure and a central Config Server.
  - What: shared configuration.
- **Step 3: Eureka**
  - Why: services should find each other by name, not fixed addresses, like a phone directory.
  - How: run a Eureka server and register services in it.
  - What: automatic service discovery.
- **Step 4: Extract User service**
  - Why: it is the simplest to separate first and builds confidence.
  - How: move user and login code into its own application.
  - What: an independent User service.
- **Step 5: Extract Catalog service**
  - Why: browsing has heavy traffic and should scale on its own.
  - How: move movies, shows, and seats into their own service.
  - What: an independent Catalog service.
- **Step 6: Extract Booking service**
  - Why: booking is the core flow and needs seat details from the Catalog.
  - How: create the Booking service and call the Catalog through OpenFeign.
  - What: services talking over the network.
- **Step 7: Payment and Notification services**
  - Why: payment and messages are separate concerns with different scaling needs.
  - How: build simple versions of both.
  - What: the full set of services exists.
- **Step 8: API Gateway**
  - Why: users need one front door, and security should be checked there once.
  - How: add the Gateway with routes and move JWT validation into it.
  - What: a single entry point.
- **Step 9: Split the databases**
  - Why: each service owning its data means one cannot break another's. The trade-off is that cross-service joins are gone.
  - How: give each service its own database and replace joins with service calls.
  - What: fully independent data.
- **Step 10: End-to-end check and tag**
  - Why: everything must still work as one system.
  - How: run full flows through the Gateway, then tag `phase-7-microservices`.
  - What: a working distributed system.

**Done when:** all flows work through the gateway and each service runs and restarts independently. Tag `phase-7-microservices`.

**Interview line:** "I split only after the monolith was stable, so I knew the boundaries. Service calls are now synchronous over Feign, which creates a new problem."

**LinkedIn posts**
- Decision: how I chose service boundaries
- Concept: what I lost when I split the monolith
- Mistake: whatever went wrong in your own split (too many services, shared database, and so on)
- Milestone: Phase 7 done, with the architecture diagram

---

## Phase 8: Kafka, Saga, and Outbox

> Note: "event" in this phase means a Kafka message (e.g. `BookingCreated`), not the `Movie` entity. This is a different, unrelated meaning of the word and is left as-is.

- **Lacking:** synchronous calls mean that if Payment is slow or fails midway, booking and payment go out of sync. A database write plus a Kafka publish can also diverge.
- **What we do:** event-driven booking flow, saga with compensation, outbox pattern, idempotency keys on the booking and payment APIs, idempotent consumers, retries, and DLQ.
- **What we achieve:** consistent data across services with no lost or duplicate messages.
- **Concepts to know first:**
  - Kafka basics: topics, partitions, offsets, consumer groups, brokers
  - Ordering guarantees and partition keys
  - Delivery semantics: at-most-once, at-least-once, exactly-once
  - Producer settings: acks, retries, idempotent producer
  - Saga: orchestration vs choreography
  - Outbox pattern and the dual-write problem
  - Idempotency
  - Idempotent vs safe HTTP methods (GET, PUT, DELETE vs POST)
  - Idempotency-Key header pattern (as used by payment providers)
  - Unique constraint as the last line of defense, and handling concurrent duplicate requests
  - Key expiry (TTL)
  - CAP theorem and eventual consistency
  - Dead letter queue and retry strategies

**Steps**

- **Step 1: Add Kafka**
  - Why: services need to pass messages without waiting for each other, like leaving notes in a mailbox.
  - How: add Kafka to docker-compose and learn topics, partitions, consumer groups, and offsets.
  - What: Kafka running and understood.
- **Step 2: First event**
  - Why: start small before changing the real flow.
  - How: publish one event from one service and consume it in another.
  - What: messages flowing.
- **Step 3: Booking as events**
  - Why: booking, payment, and confirmation become independent steps instead of one long chain of calls.
  - How: publish BookingCreated, PaymentCompleted, and BookingConfirmed events.
  - What: a working event-driven happy path.
- **Step 4: Failure path and saga**
  - Why: with no shared transaction, a failure needs an explicit undo. If payment fails, the seat must be released.
  - How: publish failure events and add compensating actions.
  - What: failures reverse cleanly.
- **Step 5: Reproduce message loss**
  - Why: saving to the database and sending a message are two separate actions, and a crash between them loses the message.
  - How: crash the service right after the save and observe.
  - What: proof of the gap.
- **Step 6: Outbox pattern**
  - Why: it guarantees the message is never lost. The message is saved with the data, and a publisher sends it afterward.
  - How: add an outbox table and a publisher that reads it.
  - What: no lost messages.
- **Step 7: Idempotency keys on booking and payment APIs**
  - Why: a user double-clicks "Pay", or the app retries after a timeout, and the customer is charged twice. Like a cashier who must not ring up the same sale twice just because the customer repeated the request.
  - How: the client sends a unique key with each request. The server saves the key with the result, and if the same key arrives again it returns the saved result instead of processing it again. Reject the same key with a different request, make sure two identical requests arriving together let only one through, and expire old keys after a set time.
  - What: repeating the same booking or payment request never creates a second booking or charge.
- **Step 8: Idempotent consumers**
  - Why: messages can arrive twice, and a customer should never be charged twice.
  - How: record processed message IDs and skip repeats.
  - What: duplicates are harmless.
- **Step 9: Retries and dead letter queue**
  - Why: a bad message should not block everything, and failed ones should be kept for review.
  - How: retry with delays, then move to a DLQ.
  - What: problem messages are isolated.
- **Step 10: Failure tests and tag**
  - Why: the guarantees must survive real crashes.
  - How: kill services mid-flow and check data stays consistent, then tag `phase-8-kafka-saga`.
  - What: a checkpoint with proven consistency.

**Done when:** killing a service mid-flow never leaves permanent inconsistency. Tag `phase-8-kafka-saga`.

**Interview line:** "There is no distributed transaction, so I used a saga. The outbox guarantees the message is never lost, idempotency keys make client retries safe on the APIs, and idempotent consumers make sure a message is never processed twice."

**LinkedIn posts**
- Bug story: the booking was saved but the Kafka message was lost, and the outbox fix
- Bug story: a double-click charged my customer twice, and how an idempotency key fixed it
- Concept: API idempotency vs consumer idempotency
- Decision: saga orchestration vs choreography
- Concept: why there is no distributed transaction

---

## Phase 9: Resilience

- **Lacking:** if the payment gateway is slow or down, threads pile up in Booking and the failure spreads.
- **What we do:** Resilience4j circuit breaker, retry with backoff, bulkhead, time limiter, fallbacks, and gateway rate limiting.
- **What we achieve:** one failing service no longer brings down the others.
- **Concepts to know first:**
  - Cascading failure and how it spreads
  - Timeouts, retries, and exponential backoff with jitter
  - Circuit breaker states: closed, open, half-open
  - Bulkhead and thread isolation
  - Fallbacks and graceful degradation
  - Idempotency (retries are safe only on idempotent calls)
  - Rate limiting at the gateway

**Steps**

- **Step 1: Simulate a bad Payment service**
  - Why: we should see failure before defending against it.
  - How: make Payment slow, then make it fail.
  - What: a way to trigger the problem.
- **Step 2: Observe the spread**
  - Why: one slow service can freeze others, like one blocked checkout lane backing up the whole shop.
  - How: watch Booking's waiting requests pile up.
  - What: proof that failures spread.
- **Step 3: Timeouts**
  - Why: no call should wait forever.
  - How: set timeouts on Feign and HTTP clients.
  - What: calls give up on time.
- **Step 4: Retry**
  - Why: brief glitches often disappear on a second try.
  - How: retry with growing delays, only for safe repeatable calls.
  - What: temporary errors recover on their own.
- **Step 5: Circuit breaker**
  - Why: repeated calls to a dead service waste resources. Like a fuse, it stops calls for a while.
  - How: add Resilience4j and tune the failure thresholds.
  - What: failing services are avoided quickly.
- **Step 6: Bulkhead and time limiter**
  - Why: one dependency should not use up all threads, just as ship compartments keep a leak from sinking the ship.
  - How: limit concurrent calls per dependency and cap the total call time.
  - What: damage stays contained.
- **Step 7: Fallbacks**
  - Why: users deserve a friendly answer, not an error page.
  - How: return a clear message like "Payment is busy, try again shortly".
  - What: graceful degradation.
- **Step 8: Gateway rate limiting**
  - Why: it protects the whole system from traffic floods.
  - How: limit requests per user at the Gateway using Redis.
  - What: overload protection.
- **Step 9: Load test and tag**
  - Why: resilience must be proven under stress.
  - How: run a load test with Payment down and check the rest stays healthy, then tag `phase-9-resilience`.
  - What: a checkpoint with proven fault tolerance.

**Done when:** with Payment down, browsing and other features stay healthy and users get a clear message. Tag `phase-9-resilience`.

**Interview line:** "A failing dependency now degrades one feature instead of taking down the booking service."

**LinkedIn posts**
- Bug story: one slow Payment service froze my Booking service
- Concept: circuit breaker explained like a fuse in your house
- Mistake: I added retries and made the outage worse
- Before and after numbers: Booking stayed healthy while Payment was down

---

## Phase 10: Observability

- **Lacking:** a request crosses five services and we cannot tell where it failed or slowed down.
- **What we do:** metrics, dashboards, distributed tracing, correlation IDs, centralized logs, and alerts.
- **What we achieve:** the ability to see where a request slowed down or failed.
- **Concepts to know first:**
  - Metrics vs logs vs traces
  - RED metrics (rate, errors, duration) and USE metrics
  - Roles of Micrometer, Prometheus, and Grafana
  - Trace ID and span ID, and how they propagate
  - Correlation IDs and structured logging
  - Percentiles: p50, p95, p99
  - Alerting basics
  - Liveness vs readiness

**Steps**

- **Step 1: Metrics**
  - Why: we cannot fix what we cannot see, like a car dashboard.
  - How: expose metrics with Actuator and Micrometer.
  - What: the app reports its own health and numbers.
- **Step 2: Prometheus**
  - Why: numbers need collecting over time to show trends.
  - How: configure Prometheus to scrape every service.
  - What: stored metrics history.
- **Step 3: Grafana dashboards**
  - Why: charts are easier to read than raw numbers.
  - How: build dashboards for latency, errors, JVM, and Kafka lag.
  - What: a visual control room.
- **Step 4: Distributed tracing**
  - Why: one booking crosses many services, and tracing follows it end to end, like a parcel tracking number.
  - How: add Micrometer Tracing with Zipkin.
  - What: a full journey for each request.
- **Step 5: Correlation IDs and structured logs**
  - Why: logs from different services should be linkable to the same request.
  - How: attach the trace ID to every log line in a consistent format.
  - What: searchable, connected logs.
- **Step 6: Centralized logs (optional)**
  - Why: reading logs on each server separately does not scale.
  - How: ship logs to ELK.
  - What: one place to search all logs.
- **Step 7: Alerts**
  - Why: the team should hear about problems before customers do.
  - How: add basic rules like high error rate or slow responses.
  - What: automatic warnings.
- **Step 8: Fault-finding drill and tag**
  - Why: it proves the tools actually help.
  - How: inject a slow call, find it using the trace, then tag `phase-10-observability`.
  - What: a checkpoint with a proven debugging ability.

**Done when:** you can locate the cause of an injected failure in under two minutes. Tag `phase-10-observability`.

**Interview line:** "I traced one slow booking to a specific Feign call using the trace ID."

**LinkedIn posts**
- Bug story: finding a slow call across five services using one trace ID
- Concept: metrics vs logs vs traces, with a car dashboard example
- Concept: why average latency lies and p99 matters

---

## Phase 11: Testing and Delivery

- **Lacking:** changes can break things silently, setup is manual, and there is no proof of performance.
- **What we do:** unit, slice, and integration tests with Testcontainers, load tests, Docker, CI, and Kubernetes basics.
- **What we achieve:** a reliable, repeatable build and deployment with proof of performance.
- **Concepts to know first:**
  - Testing pyramid: unit, integration, end-to-end
  - JUnit 5 and Mockito: mocks vs stubs vs spies
  - Slice tests: `@WebMvcTest`, `@DataJpaTest`
  - Testcontainers, and why not H2
  - Testing async and concurrent code
  - Load testing basics: throughput, latency, ramp-up
  - Docker: images, layers, multi-stage builds
  - CI/CD and GitHub Actions
  - Kubernetes: pod, deployment, service, ConfigMap, Secret
  - Zero-downtime deployment: rolling update

**Steps**

- **Step 1: Unit tests**
  - Why: small tests catch mistakes early and cheaply.
  - How: cover the booking logic with JUnit 5 and Mockito.
  - What: the core logic is protected.
- **Step 2: Slice tests**
  - Why: each layer can be tested on its own, quickly.
  - How: use `@WebMvcTest` and `@DataJpaTest`.
  - What: fast layer-level checks.
- **Step 3: Integration tests**
  - Why: real databases behave differently from fakes.
  - How: use Testcontainers for Postgres, Kafka, and Redis.
  - What: tests against real systems.
- **Step 4: Concurrency test in Testcontainers**
  - Why: the double-booking test should run against a real database.
  - How: move the Phase 4 test into Testcontainers.
  - What: a trustworthy locking test.
- **Step 5: Load tests**
  - Why: we need proof of how much traffic the system handles.
  - How: run k6 or JMeter and record the results.
  - What: performance numbers to quote.
- **Step 6: Docker**
  - Why: "works on my machine" should never be an excuse.
  - How: write a Dockerfile for each service and one docker-compose for everything.
  - What: one command starts the whole system.
- **Step 7: CI pipeline**
  - Why: every change should be built and tested automatically.
  - How: set up GitHub Actions to build, test, and create images.
  - What: an automatic quality gate.
- **Step 8: Kubernetes basics**
  - Why: real deployments run on orchestrators that restart and scale services.
  - How: write Deployment, Service, ConfigMap, and Secret files and run them on minikube or kind.
  - What: the system running on a local cluster.
- **Step 9: Final README and tag**
  - Why: the project should explain itself to interviewers.
  - How: write a README with an architecture diagram and phase notes, then tag `phase-11-delivery`.
  - What: a finished, presentable project.

**Done when:** a push triggers build and tests, an image is produced, and one command starts the whole system. Tag `phase-11-delivery`.

**Interview line:** "Every push builds, runs integration tests against real containers, and produces an image."

**LinkedIn posts**
- Decision: why Testcontainers instead of H2
- Before and after numbers: load test results (requests per second, p95 latency)
- Mistake: a test that passed locally but failed in CI
- Milestone: final project walkthrough, with architecture diagram, README, and GitHub link

---

# Reference: HLD and LLD concepts covered

## LLD (Low-Level Design)

- **SOLID:** single responsibility (controller, service, repository), dependency inversion (Spring injection), open/closed (new payment methods without editing old code)
- **Patterns:** Strategy (payment methods, notification channels), Factory, Builder, Observer (booking events triggering notifications), State (seat and booking lifecycle), Proxy (`@Transactional`, caching), Chain of Responsibility (filter chain), Singleton (Spring beans)
- **Class and schema design:** entity relationships, seat state machine, DTO vs entity, exception hierarchy, idempotent API design
- **Classic problems built along the way:** seat booking system, rate limiter, notification service, cache and distributed lock design

## HLD (High-Level Design)

- **Architecture:** monolith vs microservices, service boundaries, database per service, API Gateway, service discovery
- **Scalability:** horizontal vs vertical scaling, load balancing, stateless services, caching strategies, indexing, pagination, connection pooling
- **Data and consistency:** ACID, isolation levels, optimistic vs pessimistic vs distributed locks, CAP theorem, eventual consistency, saga, outbox, idempotency
- **Communication:** synchronous vs asynchronous, event-driven design, Kafka partitions and ordering, polling vs WebSocket vs webhook
- **Reliability:** circuit breaker, retry, timeout, bulkhead, fallback, rate limiting, graceful degradation
- **Operations:** metrics, logs, traces, health checks, alerting, containers, Kubernetes basics, CI/CD
- **Classic HLD problems this project answers:** design BookMyShow, a notification system, a rate limiter, a payment flow

---

# Progress tracker

Tick a box when the step works and is committed. Update "Current position" at the top.

## Phase 1: Basic CRUD
- [x] Step 1: Project setup
- [x] Step 2: Database structure with Flyway
- [x] Step 3: Entities and repositories
- [x] Step 4: Movie features
- [x] Step 5: Show features
- [x] Step 6: Seat viewing
- [x] Step 7: Booking
- [x] Step 8: Cancel booking
- [x] Step 9: Basic tests and tag `phase-1-basic`

## Phase 2: Transactions and Error Handling
- [x] Step 1: Reproduce the half-saved bug
- [x] Step 2: Add transactions
- [x] Step 3: Self-invocation pitfall
- [x] Step 4: Rollback rules and propagation
- [x] Step 5: Custom exceptions
- [x] Step 6: Global error handling
- [x] Step 7: DTOs and validation
- [x] Step 8: Profiles and configuration
- [x] Step 9: Transaction tests and tag `phase-2-transactions`

## Phase 3: Database Performance
- [x] Step 1: Create large data
- [x] Step 2: Reproduce the N+1 problem
- [x] Step 3: Record baseline numbers
- [x] Step 4: Fix N+1
- [x] Step 5: Lighter read-only results
- [x] Step 6: Pagination and sorting
- [x] Step 7: Indexes
- [x] Step 8: Connection pool tuning
- [x] Step 9: Comparison and tag `phase-3-performance`

## Phase 4: Concurrency and Locking
- [x] Step 1: Multithreaded test
- [x] Step 2: Observe the failure
- [x] Step 3: Optimistic locking
- [x] Step 4: Handle the conflict
- [x] Step 5: Pessimistic locking
- [x] Step 6: Timeouts and deadlocks
- [x] Step 7: Compare both
- [x] Step 8: Document and tag `phase-4-locking`

## Phase 5: Security (JWT and Roles)
- [ ] Step 1: Users, roles, and passwords
- [ ] Step 2: Registration
- [ ] Step 3: Login with JWT
- [ ] Step 4: JWT filter
- [ ] Step 5: Access rules by URL
- [ ] Step 6: Access rules on methods
- [ ] Step 7: Bookings belong to users
- [ ] Step 8: Refresh token and logout
- [ ] Step 9: Security tests and tag `phase-5-security`

## Phase 6: Redis (Caching and Seat Hold)
- [ ] Step 1: Add Redis
- [ ] Step 2: Cache the catalog
- [ ] Step 3: Cache invalidation
- [ ] Step 4: Cache problems
- [ ] Step 5: Seat hold
- [ ] Step 6: Connect the hold to booking
- [ ] Step 7: Distributed lock
- [ ] Step 8: Rate limiting on login
- [ ] Step 9: Measure and tag `phase-6-redis`

## Phase 7: Microservices Split
- [ ] Step 1: Define boundaries
- [ ] Step 2: Multi-module project and Config Server
- [ ] Step 3: Eureka
- [ ] Step 4: Extract User service
- [ ] Step 5: Extract Catalog service
- [ ] Step 6: Extract Booking service
- [ ] Step 7: Payment and Notification services
- [ ] Step 8: API Gateway
- [ ] Step 9: Split the databases
- [ ] Step 10: End-to-end check and tag `phase-7-microservices`

## Phase 8: Kafka, Saga, and Outbox
- [ ] Step 1: Add Kafka
- [ ] Step 2: First event
- [ ] Step 3: Booking as events
- [ ] Step 4: Failure path and saga
- [ ] Step 5: Reproduce message loss
- [ ] Step 6: Outbox pattern
- [ ] Step 7: Idempotency keys on booking and payment APIs
- [ ] Step 8: Idempotent consumers
- [ ] Step 9: Retries and dead letter queue
- [ ] Step 10: Failure tests and tag `phase-8-kafka-saga`

## Phase 9: Resilience
- [ ] Step 1: Simulate a bad Payment service
- [ ] Step 2: Observe the spread
- [ ] Step 3: Timeouts
- [ ] Step 4: Retry
- [ ] Step 5: Circuit breaker
- [ ] Step 6: Bulkhead and time limiter
- [ ] Step 7: Fallbacks
- [ ] Step 8: Gateway rate limiting
- [ ] Step 9: Load test and tag `phase-9-resilience`

## Phase 10: Observability
- [ ] Step 1: Metrics
- [ ] Step 2: Prometheus
- [ ] Step 3: Grafana dashboards
- [ ] Step 4: Distributed tracing
- [ ] Step 5: Correlation IDs and structured logs
- [ ] Step 6: Centralized logs (optional)
- [ ] Step 7: Alerts
- [ ] Step 8: Fault-finding drill and tag `phase-10-observability`

## Phase 11: Testing and Delivery
- [ ] Step 1: Unit tests
- [ ] Step 2: Slice tests
- [ ] Step 3: Integration tests
- [ ] Step 4: Concurrency test in Testcontainers
- [ ] Step 5: Load tests
- [ ] Step 6: Docker
- [ ] Step 7: CI pipeline
- [ ] Step 8: Kubernetes basics
- [ ] Step 9: Final README and tag `phase-11-delivery`


## 10. Phase summary (last step of every phase)

The last step of each phase (the "comparison and tag" step) must also produce a phase summary
for `NOTES.md`, in this order:

1. **Summary table:** one row per problem, with columns Problem, Before, After, Fix (step number).
2. **Per-step notes:** one short section per step with:
  - the problem and the proof (numbers, plan, log line)
  - the fix and why it was picked
  - the alternative rejected and why
  - traps, warnings, or rules to remember
3. **Lessons:** 4 to 6 one-line takeaways.
4. **Mistakes I made:** real errors hit during the phase.
5. **Left for later:** what this phase deliberately did not solve, and which phase will.
6. **Interview answers:** 4 to 6 likely questions, each with a two-line answer.

Use the learner's real numbers, not made-up ones. Wrap the summary in four backticks so the
inner code formatting survives copying.