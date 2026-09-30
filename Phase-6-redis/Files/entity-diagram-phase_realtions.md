# Table ↔ Entity Mapping Guide (Phase 1)

A full picture of every table in the database, how it was created, and exactly how it maps to Java entity classes.

## Golden rule for this project

**Flyway SQL creates tables. `@Entity` classes only describe them.** `ddl-auto: none` means Hibernate never creates, alters, or touches the schema — it just maps Java fields onto tables that already exist.

---

## All tables in the database

| Table | Created by | Real-world meaning | Has its own `@Entity` class? |
|---|---|---|---|
| `movies` | `V1__create_movies_table.sql` | A film | Yes — `Movie.java` |
| `shows` | `V2__create_shows_table.sql` | One screening of a movie | Yes — `Show.java` |
| `seats` | `V3__create_seats_table.sql` | One physical seat, scoped to a show | Yes — `Seat.java` |
| `bookings` | `V4__create_bookings_table.sql` | A customer's reservation | Yes — `Booking.java` |
| `booking_seats` | `V4__create_bookings_table.sql` | Just links bookings ↔ seats — no meaning of its own | **No** — hidden behind `@JoinTable` on `Booking.seats` |
| `flyway_schema_history` | Flyway itself | Tracks which migrations have run | No — internal to Flyway, never touched by JPA |

5 real tables + 1 Flyway bookkeeping table = 6 total. Only 4 have a matching Java class — `booking_seats` is pure plumbing.

---

## Table → Entity field mapping

### `movies` → `Movie.java`
| Column | Java field | Annotation |
|---|---|---|
| `id` | `id` | `@Id @GeneratedValue` |
| `name` | `name` | `@Column` (implicit, name matches) |
| `description` | `description` | `@Column` (implicit) |
| `created_at` | `createdAt` | `@Column(name = "created_at")` |
| *(no column)* | `shows` | `@OneToMany(mappedBy = "movie")` — not a real column, filled by querying `shows` where `movie_id` matches |

### `shows` → `Show.java`
| Column | Java field | Annotation |
|---|---|---|
| `id` | `id` | `@Id @GeneratedValue` |
| `movie_id` | `movie` | `@ManyToOne @JoinColumn(name = "movie_id")` — real FK column, but Java sees the whole `Movie` object, not a raw ID |
| `show_time` | `showTime` | `@Column(name = "show_time")` |
| `created_at` | `createdAt` | `@Column(name = "created_at")` |
| *(no column)* | `seats` | `@OneToMany(mappedBy = "show")` — filled by querying `seats` where `show_id` matches |

### `seats` → `Seat.java`
| Column | Java field | Annotation |
|---|---|---|
| `id` | `id` | `@Id @GeneratedValue` |
| `show_id` | `show` | `@ManyToOne @JoinColumn(name = "show_id")` |
| `seat_number` | `seatNumber` | `@Column(name = "seat_number")` |
| `status` | `status` | `@Enumerated(EnumType.STRING)` — stored as text (`AVAILABLE`/`BOOKED`), mapped to the `SeatStatus` enum |
| `price` | `price` | `@Column` (implicit) |

### `bookings` → `Booking.java`
| Column | Java field | Annotation |
|---|---|---|
| `id` | `id` | `@Id @GeneratedValue` |
| `show_id` | `show` | `@ManyToOne @JoinColumn(name = "show_id")` |
| `customer_name` | `customerName` | `@Column(name = "customer_name")` |
| `customer_email` | `customerEmail` | `@Column(name = "customer_email")` |
| `total_amount` | `totalAmount` | `@Column(name = "total_amount")` |
| `status` | `status` | `@Enumerated(EnumType.STRING)` → `BookingStatus` enum |
| `created_at` | `createdAt` | `@Column(name = "created_at")` |
| *(no column — separate table)* | `seats` | `@ManyToMany @JoinTable(name = "booking_seats", ...)` — see below |

### `booking_seats` → no entity class, only referenced via `@JoinTable`
| Column | Meaning |
|---|---|
| `booking_id` | FK → `bookings.id` (`joinColumns` in `Booking.java`) |
| `seat_id` | FK → `seats.id` (`inverseJoinColumns` in `Booking.java`) |

No Java class (`BookingSeat.java`) exists for this table. Hibernate reads/writes rows into it automatically whenever `booking.getSeats()` / `booking.setSeats(...)` is used — it's invisible plumbing, not a domain object you interact with directly.

---

## Foreign key chain (who points to whom)

```
movies (id)
   ↑
shows (movie_id → movies.id)
   ↑
seats (show_id → shows.id)
   ↑
booking_seats (seat_id → seats.id)
   ↑
bookings (id) ← booking_seats (booking_id → bookings.id)
   ↑
shows (show_id → shows.id, also referenced directly by bookings)
```

In plain words: a seat can't exist without its show, a show can't exist without its movie, and a booking references both a show directly (`bookings.show_id`) and its seats indirectly (through `booking_seats`).

---

## Fetch type per relationship (when data actually loads)

| Relationship | Fetch type | Why |
|---|---|---|
| `Movie.shows` | `LAZY` (default) + `@JsonIgnore` | Rarely needed alongside a movie; separate `/movies/{id}/shows` endpoint exists |
| `Show.movie` | `LAZY` (explicit) + `@JsonIgnore` | Avoids pulling the movie every time a show loads |
| `Show.seats` | `LAZY` (default) + `@JsonIgnore` | Separate `/shows/{id}/seats` endpoint exists |
| `Seat.show` | `LAZY` (explicit) + `@JsonIgnore` | Avoids pulling the show every time a seat loads |
| `Booking.show` | `LAZY` (explicit) + `@JsonIgnore` | Booking response doesn't need the full show nested in |
| `Booking.seats` | `EAGER` (explicit override) | A booking is meaningless without its seats — always needed, so fetched immediately in the same query |

`LAZY` = don't fetch from the DB until something actually asks for it (and only works while the DB session is still open — this is the exact bug behind every `LazyInitializationException` you hit today).
`EAGER` = fetch immediately, no waiting — used sparingly, only where the related data is essentially always required.

---

## One-line summary

**5 real tables, 1 Flyway bookkeeping table, 4 Java entity classes.** The 5th table (`booking_seats`) has no class of its own — it's the invisible glue behind `Booking.seats`, created by SQL, read and written automatically by JPA, and never touched directly in code.
