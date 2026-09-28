## Why GET /movies is fast but with-show-count is slow (lazy loading)

- `Movie.shows` is `fetch = FetchType.LAZY`.
- `GET /movies`: Hibernate loads only the movie rows (1 query).
  The `shows` field stays an empty placeholder and is never opened.
  Result: 1 query, ~220 ms.
- `GET /movies/with-show-count`: the loop calls `movie.getShows().size()`.
  That opens the placeholder, so Hibernate runs one extra query per movie.
  Result: 1 + 10,002 queries, ~15 s.
- Lesson: lazy loading is not the bug. It saves work when shows are not needed.
  The bug is touching a lazy field inside a loop (N+1).
- N+1 meaning: 1 query for the parents (movies: fetch all moves) + N queries for the children


## Fetching only some fields of an entity (DTO projection)

Problem: the Movie entity has 4 columns (id, name, description, created_at) plus a lazy
`shows` list. A list screen needs only id and name, but findAll() loads everything.

Fix: select only the needed fields and build a small DTO directly in the query.

### The entity (what is stored)
File: entity/Movie.java

```java
@Entity
@Table(name = "movies")
@Getter
@Setter
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                       // <- used by the projection

    private String name;                   // <- used by the projection

    private String description;            // not needed by a list screen

    @Column(name = "created_at")
    private LocalDateTime createdAt;       // not needed by a list screen

    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Show> shows = new ArrayList<>();   // lazy, not needed either

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

### Step 1: Create a DTO with only the fields you need
File: dto/MovieSummaryResponse.java

```java
@Getter
@AllArgsConstructor
public class MovieSummaryResponse {
    private Long id;
    private String name;
}
```

### Step 2: Write the query with a constructor expression
File: repository/MovieRepository.java

```java
// Only id and name are selected. No entity, no description, no createdAt.
@Query("select new com.rahul.ticketbooking.dto.MovieSummaryResponse(m.id, m.name) " +
       "from Movie m order by m.id")
List<MovieSummaryResponse> findAllSummaries();
```

### How to read the query
- `select new <full class name>(...)` = build this DTO for every row.
- `m.id, m.name` = the only fields fetched, passed to the DTO constructor.
- `from Movie m` = JPQL uses the entity name (Movie), not the table name (movies).
- The SQL that runs is: select id, name from movies order by id.

### Rules that must match
1. The full package name in the query must match the DTO class exactly.
2. The order and types of the selected fields must match the DTO constructor
   (m.id -> Long id, m.name -> String name).
3. The DTO needs a constructor with those parameters (@AllArgsConstructor provides it).
4. Do NOT return entities from this query. The DTO is not managed by Hibernate.

### Result
- findAll(): id, name, description, created_at -> full Movie entities -> 1.36 MB
- findAllSummaries(): id, name only -> small DTOs -> much smaller response

### When to use
- Read-only lists and search screens: use a DTO projection.
- Updates: load the real entity, because changes to a DTO are not saved.

### Adding more fields later
To include description, add it in three places: the DTO field, the constructor
expression (m.description), and keep the order the same.

### Interview line
"I select only the columns a screen needs with a DTO projection. It reduces database
I/O, memory and payload, and I keep full entities for write operations."





## Step 9: Phase 3 comparison (10,002 movies, 50,003 shows, 500,150 seats)

---

### Step 1: Large data
- Problem: with 10 rows every query is fast, so performance bugs stay hidden.
- Fix: SQL seed script using generate_series (10k movies, 50k shows, 500k seats).
- Why SQL and not Java: inserts 500k rows in seconds. A save() loop would take minutes.
- Why not Flyway: migrations run in every environment, so fake data would reach production.
  Flyway also checksums files, so editing data later would break startup.
- Cleanup script deletes only rows named 'Seed Movie N', so hand-made data stays safe.
- Baseline: GET /movies with 10,002 flat rows took ~200 ms (1 query, no relationships touched).

### Step 2: Reproduce N+1
- Meaning: 1 query for the parents (movies) + N queries for the children (one shows query per movie).
  10,002 movies = 10,003 queries.
- Cause: Movie.shows is LAZY, and the loop called movie.getShows().size() for every movie.
- Result: ~10,003 statements, ~15 s (with SQL printing on), against 1 query for GET /movies.
- Why GET /movies was fast: it never touched shows, so the lazy placeholders were never opened.
  Notice board example: reading names only vs opening every sealed envelope.
- Two conditions are needed for N+1: the child loads separately from the parent, AND code reads
  the child for many parents. Lazy only sets the stage. The loop triggers it.
- @Transactional(readOnly = true) was required because open-in-view is off.
  Without it the lazy access throws LazyInitializationException.
- Tools: show-sql and generate_statistics (Session Metrics shows the JDBC statement count).


### Step 3: Fix N+1
- Fix: fetch join in one query (left join fetch m.shows). The entity stays LAZY.
- Result: 10,003 queries -> 1 query, 11.75 s -> 700 ms.
- left join keeps movies with no shows. A plain join would drop them.
- Hibernate 6 removes duplicate parents automatically, so distinct is not needed.
- Fetch join vs @EntityGraph: same SQL and same speed. Use a fetch join when already writing a
  @Query. Use @EntityGraph to add loading to a derived method without writing JPQL.
- Why not EAGER: with findAll() it still runs one shows query per movie, so GET /movies also
  becomes slow and all shows go into memory. Rule: lazy by default, fetch explicitly where needed.
- Why still 700 ms: the fetch join loads all 50,003 shows just to count them.
- Reproduce N+1 again: use movieRepository.findAll() instead of findAllWithShows().

### Step 4: DTO projection
- Problem: a list screen needs id and name, but findAll() returns full entities.
- Fix: JPQL constructor expression: select new ...MovieSummaryResponse(m.id, m.name) from Movie m.
- Rules: the full package name must match, and field order and types must match the constructor.
- Benefit: fewer columns read, no entities created, no dirty-checking, smaller response.
- Result: 1.36 MB -> 380 KB. Time barely moved (134 ms) because all 10,002 rows are still processed.
- Use projections for read-only lists. Load the real entity when updating, because a DTO is not managed.
- In JPQL, m is an alias for Movie. Use entity and field names, not table and column names.

### Step 5: Pagination and sorting
- Problem: returning all 10,002 rows when a screen shows about 20.
- Fix: Pageable in, Page out. Spring adds limit, offset and order by from page, size and sort in the URL.
- Result: 134 ms -> 14 ms and 380 KB -> 1.1 KB.
- Lesson: projection cuts row width, pagination cuts row count. Use both.
- Page runs a second query (countQuery) to get totalElements and totalPages. The count has no limit
  and returns one number. Its where clause must match the main query.
- If a page comes back with fewer rows than the page size on page 0, Spring skips the count query.
- Slice skips the count entirely and only says whether a next page exists. Good for infinite scroll.
- Guardrail: max-page-size 100 so a client cannot request size=1000000.
- Offset paging gets slower on deep pages because the database still walks past skipped rows.
  Keyset paging (where id > lastId) stays fast but cannot jump to page N.
- Fetch join with pagination makes Hibernate paginate in memory (warning HHH90003004),
  which is why we paginate the projection instead.
- Kept both endpoints (/summary/all and /summary) so before and after can be compared without editing code.

### Step 6: Indexes
- Problem: searching by name scanned every row.
- Proof before: Seq Scan, Rows Removed by Filter 10001, 142 buffers, 1.299 ms.
- Fix: CREATE INDEX idx_movies_name ON movies (name) in Flyway V8__add_index_movies_name.sql.
- Proof after: Index Scan, 3 buffers, 0.059 ms. Index Cond replaces Filter.
- Buffers are a more honest number than time at 10k rows, because timings are noisy and cached.
- Index ignored: LIKE '%text%' (leading wildcard) cannot use a B-tree. A prefix search can.
  Trigram or full text indexes handle the middle-of-word case.
- Cost: every insert and update must also update the index, so add indexes only where searches need them.
- The migration is permanent. Never edit or delete an applied Flyway file. Add a new version instead.
- Mistake: the file was named V8_ (one underscore) and Flyway silently ignored it. It needs V8__.
- Postman before/after: drop the index, measure, recreate it, run ANALYZE.
- shows.movie_id and seats.show_id were