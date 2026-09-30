package com.rahul.ticketbooking.security;

import com.jayway.jsonpath.JsonPath;
import com.rahul.ticketbooking.entity.Role;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Phase 5, Step 9: proves the access rules from Steps 1 to 8.
 * Needs Postgres running (docker compose up -d). Uses random emails, so it can be re-run any time.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityFlowTest {

    private static final String PASSWORD = "Test@12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String buyerEmail;
    private String buyerToken;
    private String strangerToken;
    private String organizerToken;
    private String adminToken;
    private Long showId;
    private long sharedBookingId;   // owned by the buyer, used by the read and stranger tests

    @BeforeAll
    void setUp() throws Exception {
        buyerEmail = register("buyer");
        buyerToken = login(buyerEmail);
        strangerToken = login(register("stranger"));
        organizerToken = login(createUserWithRole("organizer", Role.ORGANIZER));
        adminToken = login(createUserWithRole("admin", Role.ADMIN));

        long movieId = createMovie(organizerToken);
        showId = createShow(organizerToken, movieId);
        sharedBookingId = bookOneSeat(buyerToken);
    }

    // ---------- Public access and 401 ----------

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        mockMvc.perform(get("/movies")).andExpect(status().isOk());
        mockMvc.perform(get("/shows/" + showId + "/seats")).andExpect(status().isOk());
        mockMvc.perform(get("/public")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void tamperedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(buyerToken + "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noTokenCannotReadBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/" + sharedBookingId))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Register and login ----------

    @Test
    void registerIgnoresRoleSentByClient() throws Exception {
        String email = "sneaky-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Sneaky", "email", email, "password", PASSWORD, "role", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void duplicateEmailReturns409EvenWithDifferentCase() throws Exception {
        String email = register("dupe");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Dupe", "email", email.toUpperCase(), "password", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void wrongPasswordAndUnknownEmailBothReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", buyerEmail, "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "nobody-" + UUID.randomUUID() + "@test.com", "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    // ---------- Roles (403) ----------

    @Test
    void userCannotCreateMovieOrShow() throws Exception {
        mockMvc.perform(post("/movies")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Not Allowed", "description", "x")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(post("/movies/1/shows")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("showTime", "2030-01-01T18:00:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizerAndAdminCanCreateMovies() throws Exception {
        createMovie(organizerToken);   // helper asserts 201
        createMovie(adminToken);
    }

    // ---------- Booking and ownership ----------

    @Test
    void userCanBookAndIdentityComesFromToken() throws Exception {
        mockMvc.perform(post("/api/bookings/" + showId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatBody(firstAvailableSeatId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerEmail").value(buyerEmail))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void ownerCanReadOwnBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/" + sharedBookingId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerEmail").value(buyerEmail));
    }

    @Test
    void strangerCannotReadBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/" + sharedBookingId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void strangerCannotCancelBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/" + sharedBookingId + "/cancel")
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden());

        // The booking must be untouched.
        mockMvc.perform(get("/api/bookings/" + sharedBookingId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void adminCanReadAnyBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/" + sharedBookingId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanCancelBooking() throws Exception {
        long ownBookingId = bookOneSeat(buyerToken);
        mockMvc.perform(post("/api/bookings/" + ownBookingId + "/cancel")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ---------- Refresh and logout ----------

    @Test
    void refreshRotatesTokenAndOldTokenStopsWorking() throws Exception {
        String loginBody = loginBody(register("refresher"));
        String oldRefresh = JsonPath.read(loginBody, "$.refreshToken");

        String refreshBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", oldRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String newRefresh = JsonPath.read(refreshBody, "$.refreshToken");
        assertNotEquals(oldRefresh, newRefresh);

        // The old token was used once, so it is dead now.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", oldRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String loginBody = loginBody(register("leaver"));
        String refreshToken = JsonPath.read(loginBody, "$.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Helpers ----------

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", prefix, "email", email, "password", PASSWORD)))
                .andExpect(status().isCreated());
        return email;
    }

    // Registration only creates USERs, so ORGANIZER and ADMIN are saved directly in the database.
    private String createUserWithRole(String prefix, Role role) {
        String email = prefix + "-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setName(prefix);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return email;
    }

    private String loginBody(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String login(String email) throws Exception {
        return JsonPath.read(loginBody(email), "$.accessToken");
    }

    private long createMovie(String token) throws Exception {
        String response = mockMvc.perform(post("/movies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "SecTest-" + UUID.randomUUID(), "description", "test")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idFrom(response);
    }

    private long createShow(String token, long movieId) throws Exception {
        String response = mockMvc.perform(post("/movies/" + movieId + "/shows")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("showTime", "2030-01-01T18:00:00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idFrom(response);
    }

    private long bookOneSeat(String token) throws Exception {
        String response = mockMvc.perform(post("/api/bookings/" + showId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatBody(firstAvailableSeatId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idFrom(response);
    }

    private long firstAvailableSeatId() throws Exception {
        String response = mockMvc.perform(get("/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(response, "$[?(@.status == 'AVAILABLE')].id");
        return ids.get(0).longValue();
    }

    private long idFrom(String response) {
        Number id = JsonPath.read(response, "$.id");
        return id.longValue();
    }

    private String seatBody(long seatId) {
        return "{\"seatIds\":[" + seatId + "]}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // Builds a flat JSON object from key, value pairs. Enough for our string-only request bodies.
    private String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(keyValues[i]).append("\":\"").append(keyValues[i + 1]).append("\"");
        }
        return sb.append("}").toString();
    }
}