# Phase 6 in Stories: Redis (Caching and Seat Hold)

Redis is the notice board next to the cinema's main ledger. The ledger (Postgres) is the official record. The board (Redis) is fast to read and easy to wipe, and we use it for notes that do not need to last.

Every Redis feature in this phase expires by itself. That one idea (TTL) is the thread that ties the phase together.

---

## Step 1: Add Redis

**Story:** The cinema counter gets a notice board next to the ledger. Writing on the board is quick, and anything on it can be wiped without harm.

**Without it:** every bit of temporary information has to go into the ledger, which is slower and clutters the official record.

**What we used:**

```java
redisTemplate.opsForValue().set("ping", "pong", Duration.ofSeconds(60));
```

- `spring-boot-starter-data-redis` and `StringRedisTemplate`
- The key expired by itself after 60 seconds.
- No volume for Redis on purpose. The database stays the source of truth.

---

## Step 2: Cache the catalog

**Story:** 500 people walk in and ask "what movies are playing?". The clerk walks to the back office and copies the list from the ledger for every single person. After the first person, he should have left a copy on the counter.

**Without it:** every browse request runs a database query, even though the movie list rarely changes.

**What we used:**

```java
@Cacheable(cacheNames = "movieSummaryPage", key = "...page, size, sort...")
```

- First call: miss (reads Postgres, saves to Redis). Next calls: hits.
- Stored as JSON with a 10-minute TTL.
- We cache `PageResponse` and DTOs, never the entity.

---

## Step 3: Cache invalidation

**Story:** The organizer adds "Avengers" to the ledger, but the copy on the counter does not list it. Customers keep seeing the old list for ten minutes. The clerk must throw away the old copy the moment the ledger changes.

**Without it:** after a new movie was created, `totalElements` did not change.

**What we used:**

```java
@CacheEvict(cacheNames = "movieSummaryPage", allEntries = true)
public Movie createMovie(Movie movie)
```

- `allEntries = true`, because a new movie can change every page and the count.
- The TTL stays as a safety net.

---

## Step 4: Cache problems

**Story A, stampede:** The counter copy expires at exactly 6 PM, and 1,000 people ask at that moment. All 1,000 see "no copy" and all 1,000 run to the back office. The fix is that one clerk goes, and the others wait for his copy.

**Story B, avalanche:** The clerk made ten copies in the morning, all with a 10-minute life. At 10:10 they all vanish together, and the back office gets a wave. The fix is to give every copy a slightly different life.

**Story C, penetration:** Someone keeps asking for "movie number 999999", which does not exist. The counter never has a copy of a movie that does not exist, so every question goes to the back office. The fix is a sticky note saying "no such movie" for one minute.

**What we used:**

```java
@Cacheable(cacheNames = "movieSummaryPage", sync = true, ...)   // stampede
.entryTtl((key, value) -> calculateTtl(value))                  // 10 min + random 0-120 s, 1 min for "not found"
```

- Measured: many cache misses before `sync = true`, one after.
- Limit: `sync = true` protects one app copy only. For the whole system, a distributed lock is needed.

---

## Step 5: Seat hold

**Story:** Alice picks seat A1 and goes to the payment desk. It takes her three minutes. Meanwhile Bob also wants A1. The clerk puts a "reserved for Alice" card on the seat, and the card is thrown away automatically after a few minutes. If Alice and Bob reach the seat in the same instant, only one card can be put down.

**Without it:** Bob can take the seat while Alice is paying. Or we block the seat in the database, and if Alice walks away it stays blocked forever.

**What we used:**

```java
redisTemplate.opsForValue().setIfAbsent("hold:seat:" + seatId, userId, Duration.ofSeconds(ttl));
```

- One command that means "put the card only if there is none, with an expiry". Redis runs it one at a time, so exactly one person wins. No lock is needed.
- If Alice asks for A1 and A2 and A2 is taken, her card on A1 is taken back (all or nothing).
- Only the owner can release a hold.

---

## Step 6: Connect the hold to booking

**Story:** At the ticket window the clerk now checks the card before selling. "Do you have the reservation card for A1?" If Bob has no card, or the card is Alice's, or the card expired, no ticket is sold. After the ticket is sold, the card is thrown away.

**Without it:** a hold lived only in Redis, and anyone could still book a held seat through the booking API.

**What we used:**

```java
seatHoldService.assertHeldByUser(request.getSeatIds(), user.getId());   // before booking
releaseHoldsQuietly(request.getSeatIds(), user.getId());               // after the commit
```

- If removing the card fails after the sale, the ticket is still valid. The TTL clears it anyway.
- The hold is a courtesy to the customer. The database lock is still what guarantees no double booking.

---

## Step 7: Distributed lock

**Story:** Alice holds A1 and taps "Confirm" twice by mistake. The two taps go to two different clerks in two different booths (two app copies). Both see Alice's card, so both start writing a ticket for A1. We need a "one booth at a time" sign for A1 that both booths can see. That sign is kept on the shared notice board.

**Without it:** in this app the database lock already covers this, so nothing breaks today. But once the database is split in Phase 7, a booth cannot lock a row it does not own. A sign on the shared board still works.

**What we used:**

```java
lock.tryLock(3, 10, TimeUnit.SECONDS);   // wait up to 3 s, auto-expires after 10 s
```

- Seats are locked lowest id first, so two clerks never wait on each other.
- The lock wraps the transaction: lock, book and commit, then unlock.
- Test result: one app copy got `201`, the other `503 SEAT_BUSY`.

**Which lock when:**

- **Optimistic (`@Version`):** no waiting, conflicts show as failures and retries. Best when clashes are rare.
- **Database lock (`FOR UPDATE`):** simple and strong while everything is in one database. Waiting requests hold database connections.
- **Redis lock:** lives outside the database, so it can guard work across services. It costs one more network hop and one more thing that can fail.

---

## Step 8 (planned): Rate limiting on login

**Story:** A stranger stands at the member desk and tries a thousand passwords for Alice's account. The clerk counts the wrong tries. After five in a minute, he says "come back in a minute".

**Without it:** nothing stops password guessing.

---

## Step 9 (planned): Measure and tag

**Story:** Before the cache, the clerk took a long time per question. With the copy on the counter he answers almost instantly. We write the real numbers down so we can quote them in an interview.

---

## The whole phase in one line

Redis is the notice board. We used it for **copies** (cache), **cards** (seat hold), **"one at a time" signs** (lock), and next a **tally of wrong tries** (rate limit). Every one of them expires by itself, and the ledger (Postgres) stays the official record.