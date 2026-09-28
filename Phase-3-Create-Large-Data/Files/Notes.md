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