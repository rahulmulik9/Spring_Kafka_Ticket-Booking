package com.rahul.ticketbooking.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void hashIsNotThePlainPassword() {
        String hash = encoder.encode("Secret@123");
        assertNotEquals("Secret@123", hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfSalt() {
        String hash1 = encoder.encode("Secret@123");
        String hash2 = encoder.encode("Secret@123");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void matchesWorksForRightAndWrongPassword() {
        String hash = encoder.encode("Secret@123");
        assertTrue(encoder.matches("Secret@123", hash));
        assertFalse(encoder.matches("wrong-password", hash));
    }
}