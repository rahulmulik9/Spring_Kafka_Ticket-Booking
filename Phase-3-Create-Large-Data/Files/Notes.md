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
- Fix: load movies and shows together in one query (Step 4).