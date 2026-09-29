# Phase 4: Concurrency and Locking

## Summary table

| Problem | Before | After | Fix (step) |
|---|---|---|---|
| Double booking | 30/30 threads booked the same seat | 1/30 succeeds, 29 rejected | Step 3, 5 |
| Raw lock error reaching the user | ObjectOptimisticLockingFailureException surfaced | Clean "seat already booked" | Step 4 |
| Wrong audit on lock failure | Losers logged as success | Success logged only after real commit | Step 4 |
| Pool starvation under load | Other errors > 0, connection timeouts, slow | 0 errors, 410ms (pool 10, 20 threads) | Step 6 |
| No lock timeout | A waiting request could wait forever | Gives up after 3s, returns 503 | Step 6 |
| Deadlock risk | Two seats locked in opposite order can deadlock | Always locked in ascending id order, 0 errors over 20 rounds | Step 6 |

## Per-step notes

**Step 1: Multithreaded test**
- Problem: no test existed for concurrent booking. A sequential test can never show a race condition.
- Proof/plan: ExecutorService + two CountDownLatches (ready, start) fire 30 threads at the same instant.
- Trap avoided: no @Transactional on the test itself — that would force everything onto one thread's transaction and the race could never happen.

**Step 2: Observe the failure**
- Problem: does @Transactional alone stop double booking? No.
- Proof: 30/30 threads succeeded, 30 booking rows for one seat, seat status looked completely normal (BOOKED).
- Why: check (status == AVAILABLE) and update (status = BOOKED) are two separate steps. Default isolation READ COMMITTED lets every thread read AVAILABLE before anyone commits.
- Alternative tried: REPEATABLE_READ isolation — worked on Postgres but rejected: database-specific behavior, and losers still get a technical DB error that needs handling anyway.

**Step 3: Optimistic locking**
- Fix: @Version (Long, jakarta.persistence) on Seat, migration V9.
- Proof: 1 success, 29 ObjectOptimisticLockingFailureException, 1 booking row — 3/3 runs.
- Why picked: cheap on reads, no lock held; fits "conflicts are rare" for most seats.
- Trap: version must be Long not long (null = new entity), and the import must be jakarta.persistence.Version.

**Step 4: Handle the conflict**
- Problem: the lock exception is thrown at COMMIT time, after BookingService's try/catch, so losers got a raw error and were wrongly audited as success.
- Fix: BookingFacade — separate bean, retries up to 3 times, audits success/failure only after the real outcome is known.
- Why a separate bean: a self-call inside BookingService would skip the Spring proxy (Phase 2 self-invocation trap) and reuse the same failed transaction.
- Result: 1 success, 29 "already booked" (all failed on attempt 1, succeeded in failing cleanly on attempt 2).
- Rule: retry ONLY the lock exception, never business errors — they fail the same way every time.

**Step 5: Pessimistic locking**
- Fix: SeatRepository.findAllByIdForUpdate — SELECT ... FOR UPDATE, ORDER BY id.
- Proof: same result (1 success, 29 already booked), but zero "Seat conflict" retry lines in the log — losers wait, then read the fresh row and are rejected cleanly on the first attempt.
- Trade-off: waiting requests hold a DB connection each.

**Step 6: Timeouts and deadlocks**
- Problem 1: AuditService.logAttempt used REQUIRES_NEW and was called from inside BookingService's transaction. That needed a 2nd connection while the 1st connection + seat lock were held. With pool=10, 20 threads on one seat starved the pool.
- Proof: PoolStarvationTest before fix — red, other errors > 0. After fix — green, 1 success, 19 already booked, 0 errors, 410ms.
- Fix: moved ALL audit calls (success and failure) into BookingFacade, after BookingService's transaction ends. BookingService now uses exactly one connection per call.
- Problem 2: no lock_timeout — a waiting request could wait forever.
- Fix: SET lock_timeout = 3000 via Hikari connection-init-sql. PessimisticLockingFailureException → 503 SEAT_LOCK_TIMEOUT.
- Problem 3: deadlock risk when two users lock the same two seats in opposite order across separate statements.
- Proof: reproduced manually in psql (two sessions, opposite lock order) — Postgres killed one with "deadlock detected".
- Fix: seats are always locked in one query with ORDER BY id, so every request takes the lowest id first — no cycle possible. DeadlockTest: 20/20 OK, 20/20 rejected, 0 errors, over 20 rounds.

**Step 7: Compare both**
- Refactor: BookingService.book() and BookingFacade.attempt() hold the shared logic once; createBooking/createBookingOptimistic only differ in which fetch/call they pass in (Function/BiFunction).
- Heavy (1 seat, 30 threads): Pessimistic 1 success/29 rejected/664ms. Optimistic 1 success/29 rejected/244ms.
- Light (30 seats, 30 threads, no real contention): Pessimistic 30/0/95ms. Optimistic 30/0/89ms.
- Surprising result: optimistic was faster under heavy contention here, not slower as the textbook claim usually goes. Losers under pessimistic queue one-at-a-time behind the row lock; losers under optimistic all race through the read and fail together at commit, with no queueing. That won on wall-clock time in this short-transaction test, though it's still 29 wasted transactions vs 29 fast clean rejections.
- Decision: kept pessimistic (createBooking) as the real booking flow, kept createBookingOptimistic only as a comparison artifact.

## Lessons
1. A test with no assertion proves nothing — Step 1's test looked fine until Step 2 added `assertEquals(1, success)`.
2. @Transactional guarantees atomicity, not isolation from other transactions reading stale data — that's what isolation levels and locking are for.
3. An exception thrown at commit time (after flush) is invisible to a try/catch wrapped around the method body — this broke both the error message (Step 4) and the audit log (Step 4/6).
4. A self-call inside the same class skips the Spring proxy — this is why the retry needed its own bean, same trap as Phase 2.
5. Locking cost is invisible in a passing test unless you specifically measure it — the pool starvation bug didn't fail DoubleBookingTest at all, it needed its own test with a small pool.
6. "Optimistic is faster on light load, pessimistic is faster on heavy load" is not a law — it depends on transaction length and what "faster" is measured on. Measure, don't assume.

## Mistakes I made
- Left the failure audit inside BookingService's transaction after adding retries, which caused pool starvation under a small pool (fixed in Step 6).
- Initially didn't add ORDER BY consistently to the FOR UPDATE query — worth double-checking this stays in place if the query is ever rewritten.
- Forgot to keep NOTES.md and PROJECT_PLAN.md tracker updated as I went, had to backfill.

## Left for later
- No retry/backoff delay yet — Phase 9 (Resilience) adds proper backoff and jitter for retries.
- Seat hold before payment (reserve for a few minutes without committing) — Phase 6 (Redis).
- This locking only works within one database. Once services split apart in Phase 7, cross-service consistency needs a different approach — saga/outbox in Phase 8.
- lock_timeout is set globally per connection (connection-init-sql) — fine for one service, but worth revisiting per-query tuning later if needed.

## Interview answers

**Q: Why doesn't @Transactional alone prevent double booking?**
A: A transaction makes a group of changes atomic, but under READ COMMITTED every transaction can still read the same "available" row before anyone commits. I proved this with 30 threads all booking the same seat — all 30 succeeded.

**Q: How does optimistic locking work?**
A: A @Version column is included in the update's WHERE clause. If another transaction already changed the row, zero rows match and Hibernate throws an exception, so only the first commit wins.

**Q: How is that different from pessimistic locking?**
A: Pessimistic locking (SELECT ... FOR UPDATE) takes the lock at read time, so other transactions wait instead of racing and failing. Optimistic never blocks a reader, it only fails late, at commit.

**Q: When would you use each?**
A: Optimistic when conflicts are rare, since it avoids the cost of blocking on every read. Pessimistic when conflicts are frequent on a specific resource, since it avoids repeated wasted work. In my load test they were actually close in wall-clock time even under heavy contention, but pessimistic avoids retry churn and connection thrashing over sustained load, which matters more in production than one burst.

**Q: How do you avoid deadlocks with row-level locks?**
A: Always acquire locks in a fixed, consistent order across the whole codebase — I use ORDER BY id in a single query so every request locks the lowest seat id first, which makes a circular wait impossible.

**Q: What happens if a request waits too long for a lock?**
A: I set Postgres's lock_timeout to 3 seconds per connection, and any PessimisticLockingFailureException (timeout, deadlock, or serialization failure) is caught centrally and returned as a 503 with a clear message, instead of hanging or returning a raw 500.