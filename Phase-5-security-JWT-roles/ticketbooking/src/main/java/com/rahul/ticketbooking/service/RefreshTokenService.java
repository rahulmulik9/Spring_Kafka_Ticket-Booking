package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.RefreshToken;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.exception.InvalidCredentialsException;
import com.rahul.ticketbooking.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    // Creates a new refresh token. The database keeps only the hash.
    // The raw token is returned once, to be sent to the client, and is never stored.
    @Transactional
    public String create(User user) {
        String rawToken = generateRawToken();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    // Validates the token and deletes it, so it can be used only once (rotation).
    // The caller then issues a brand new pair.
    @Transactional
    public User consume(String rawToken) {
        String hash = hash(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        // If two requests use the same token at the same moment, only one delete returns 1.
        int deleted = refreshTokenRepository.deleteByTokenHash(hash);
        if (deleted == 0) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        return token.getUser();
    }

    // Logout: delete the token. Unknown or already deleted tokens are ignored on purpose.
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(hash(rawToken));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}