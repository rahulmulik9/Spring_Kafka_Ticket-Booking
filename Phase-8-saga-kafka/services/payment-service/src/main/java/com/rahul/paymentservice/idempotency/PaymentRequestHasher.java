package com.rahul.paymentservice.idempotency;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PaymentRequestHasher {

    private PaymentRequestHasher() {
    }

    public static String hash(Long bookingId, BigDecimal amount) {
        // 500 and 500.00 are the same amount, so they must give the same fingerprint
        String canonical = bookingId + ":" + amount.stripTrailingZeros().toPlainString();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}