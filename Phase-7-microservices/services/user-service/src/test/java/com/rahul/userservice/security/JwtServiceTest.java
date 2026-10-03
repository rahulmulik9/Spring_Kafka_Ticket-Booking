package com.rahul.userservice.security;

import com.rahul.userservice.entity.Role;
import com.rahul.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars!!";

    private final JwtService jwtService = new JwtService(SECRET, 15);

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setEmail("a@b.com");
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void tokenCarriesEmailIdAndRole() {
        Claims claims = jwtService.parseClaims(jwtService.generateToken(user()));

        assertEquals("a@b.com", claims.getSubject());
        assertEquals(7L, ((Number) claims.get("uid")).longValue());
        assertEquals("USER", claims.get("role", String.class));
    }

    @Test
    void expirationSecondsIsMinutesTimesSixty() {
        assertEquals(900, jwtService.getExpirationSeconds());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService("another-secret-key-also-32-chars-long!!", 15);
        String foreignToken = other.generateToken(user());

        assertThrows(JwtException.class, () -> jwtService.parseClaims(foreignToken));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService alreadyExpired = new JwtService(SECRET, -1);
        String token = alreadyExpired.generateToken(user());

        assertThrows(JwtException.class, () -> jwtService.parseClaims(token));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(WeakKeyException.class, () -> new JwtService("short", 15));
    }
}