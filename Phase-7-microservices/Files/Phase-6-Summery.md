# Phase 5: Security (JWT and Roles)

## 1. Summary table

| Problem | Before | After | Fix (step) |
|---|---|---|---|
| Nobody is identified | No users, every API open | No token gives 401 with our JSON error | Steps 3, 4 |
| Passwords | No user accounts | Only BCrypt hashes stored (`$2a$10$...`, 60 chars) | Step 1 |
| Duplicate sign-up | No registration | Same email, even in a different case, gives 409 `EMAIL_ALREADY_EXISTS` | Step 2 |
| Client picks own role | Not applicable | Role sent in the body is ignored, always `USER` | Step 2 |
| Wrong role does an action | A USER could create movies and shows | USER gets 403 on create, ORGANIZER and ADMIN get 201 | Steps 5, 6 |
| Reading or cancelling another user's booking | Any caller, by changing the id | Stranger gets 403, owner and ADMIN get 200 | Step 7 |
| Session cannot be revoked | 30 minute access token, logout did nothing | 15 minute (900 s) access token, refresh token stored hashed and deleted on logout | Step 8 |
| Security only checked by hand | Manual Postman runs | 17 integration tests, all passing | Step 9 |

## 2. Per-step notes

### Step 1: Users, roles, and passwords
- Problem: the app had no idea who was calling, and `Booking` only held a name and email typed by the client.
- Fix: `users` table, one role per user as an enum stored as a string, BCrypt password hashes. Proof: `password_hash` starts with `$2a$10$`.
- Alternative rejected: a separate roles table (many roles per user). Not needed for three roles.
- Remember: the table is `users`, not `user`, because `user` is reserved in PostgreSQL. Store the enum as STRING, never ORDINAL. The dev seeder is `@Profile("dev")` only.

### Step 2: Registration
- Problem: no way for a new person to join, and a duplicate email would have surfaced as a raw 500.
- Fix: the request DTO has no role field. A friendly `existsByEmail` check, then `saveAndFlush` inside a try/catch for `DataIntegrityViolationException`, which covers two sign-ups at the same instant. Emails are trimmed and lowercased.
- Alternative rejected: relying on the code check alone, because it is not safe under concurrency.
- Remember: the UNIQUE constraint is the real guarantee. `saveAndFlush` is needed so the insert happens inside the try block. Never return the entity, because it holds `passwordHash`.

### Step 3: Login with JWT
- Problem: after login the client needs proof of identity without sending the password every time.
- Fix: jjwt with HS256. The token holds email, user id, role, issued-at, and expiry.
- Alternative rejected: Spring's Nimbus resource server. It is less code but hides more.
- Remember: a JWT is signed, not encrypted, so never put secrets in it. The secret must be at least 32 characters. Unknown email and wrong password return the same 401 message. In prod the secret must come from an environment variable.

### Step 4: JWT filter
- Problem: tokens were issued but nothing checked them.
- Fix: `JwtAuthenticationFilter` verifies the signature and expiry and puts an `AuthUser` in the SecurityContext. The session is stateless. `JwtAuthenticationEntryPoint` returns the 401 JSON.
- Alternative rejected: loading the user from the database on every request. It defeats the point of stateless tokens.
- Remember: the filter only identifies the caller, and the URL rules decide whether a caller is needed. Security rejections happen before the controller, so `@RestControllerAdvice` never sees them. The authority must be `ROLE_` plus the role name.

### Step 5: Access rules by URL
- Problem: everything needed a login, including browsing, and any logged-in user could create movies.
- Fix: public GETs for movies and seats, POST create restricted to ORGANIZER and ADMIN, everything else needs a login. `JwtAccessDeniedHandler` returns the 403 JSON.
- Alternative rejected: allow by default. Deny by default is safer, because a forgotten endpoint stays locked.
- Remember: rules are checked top to bottom and the first match wins. 401 means we do not know you, 403 means we know you but you are not allowed.

### Step 6: Access rules on methods
- Problem: URL rules are coarse and easy to get wrong, and non-HTTP callers skip them entirely.
- Fix: `@EnableMethodSecurity` and `@PreAuthorize` on the create methods. Added an `AccessDeniedException` handler to `GlobalExceptionHandler`.
- Alternative rejected: URL rules only.
- Remember: without `@EnableMethodSecurity` the annotation is silently ignored. Without the new handler the catch-all turns a denied call into a 500. `@PreAuthorize` does not run on self-invocation, because it works through a proxy.

### Step 7: Bookings belong to users
- Problem: any logged-in user could read or cancel any booking by guessing an id (IDOR), and the customer name was whatever the client typed.
- Fix: `user_id` on `bookings` (V11), identity taken from the token, `checkOwnerOrAdmin` on get and cancel.
- Alternative rejected: returning 404 for other people's bookings. It hides that the id exists, but 403 is clearer to learn from.
- Remember: the user id comes from the signed token, never from the body. `customerName` and `customerEmail` are now a snapshot taken at booking time. The facade needed the `AuthUser`, because it also logs the caller's email.

### Step 8: Refresh token and logout
- Problem: a 30 minute token could not be cancelled, so logout did nothing on the server.
- Fix: a 15 minute access token plus a random 32-byte refresh token stored as a SHA-256 hash, rotated on every use. Logout deletes the row.
- Alternative rejected: a long-lived JWT as the refresh token, because it cannot be revoked.
- Remember: `deleteByTokenHash` returns a count, which makes a double use of the same token fail. Refresh and logout are public endpoints, because the access token may already be expired. The old access token still works until it expires.

### Step 9: Security tests
- Problem: security was only checked by hand.
- Fix: `SecurityFlowTest` with 17 tests. It logs in as each role and checks the allowed cases and, more importantly, the denied ones.
- Alternative rejected: mocking security. It would skip the filter, the URL rules, and the method rules.
- Remember: the tests run against the local dev Postgres with random emails and leave some data behind. Testcontainers replaces this in Phase 11.

## 3. Lessons
- 401 means we do not know you, 403 means we know you but you are not allowed.
- URL rules and method rules are two layers, so one mistake does not open a hole.
- The identity comes from the signed token, never from the request body.
- A UNIQUE constraint is the real guard against duplicates. The code check is only for a friendly message.
- Store hashes of passwords and refresh tokens, never the raw values.
- Test the denied cases as carefully as the allowed ones.

## 4. Mistakes I made
- Sent JSON from PowerShell with escaped quotes, and the server received literal backslashes. It came back as a 500, because our catch-all handler swallowed `HttpMessageNotReadableException`. Fix: put the JSON in a file (`-d "@body.json"`) or use Postman.
- Passed `user.getId()` from the controller where `BookingFacade` expected the whole `AuthUser`. It did not compile. The facade also still read `request.getCustomerEmail()`, which no longer exists.
- Got a `LazyInitializationException` on `booking.getShow().getMovie().getName()` in `BookingMapper`. The security tests caught it, and hand testing had missed it. Fix: a fetch join (`findByIdWithShowAndMovie`, `findByIdWithMovie`).

## 5. Left for later
- The access token stays valid after logout until it expires. A denylist in Redis is planned for Phase 6.
- Login rate limiting to stop password guessing. Phase 6, Step 8.
- Expired refresh token rows are never cleaned up. A scheduled cleanup job is still to do.
- Malformed JSON returns a 500 instead of a 400. A small handler is still to do.
- The Phase 4 concurrency test is commented out. It moves to Testcontainers in Phase 11, Step 4.
- Registration reveals which emails exist. Accepted for this project.
- Security across services (gateway validation) comes in Phase 7.

## 6. Interview answers
- **How do you store passwords?** BCrypt hashes with a random salt and an adjustable cost. If the database leaks, the passwords cannot be cheaply reversed.
- **What is the difference between 401 and 403?** 401 means the caller is not authenticated. 403 means the caller is known but not allowed.
- **How does Spring Security know the user on each request?** A filter verifies the JWT and puts the user in the SecurityContext. The app is stateless, so no session is kept on the server.
- **How do you log out with JWT?** I use a short access token plus a refresh token stored hashed in the database. Logout deletes the refresh token, and the access token dies on its own within 15 minutes. A denylist would close that gap.
- **How do you stop one user reading another user's booking?** The service compares the booking's owner with the id from the token, and returns 403 if they differ. I do not trust an id from the request.
- **Why both URL and method security?** URL rules are coarse and easy to misconfigure, and non-HTTP callers skip them. Method rules travel with the code, so together they give defence in depth.