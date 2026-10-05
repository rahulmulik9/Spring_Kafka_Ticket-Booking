package com.rahul.bookingservice.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

// Turns a request into a fixed fingerprint. The seat order does not matter: [1,4] and [4,1] are the same request.
public final class BookingRequestHasher {

    private BookingRequestHasher() {
    }

    public static String hash(Long showId, List<Long> seatIds) {
        String canonical = showId + ":" + seatIds.stream()
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);   // 64 characters
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}