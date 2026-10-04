package com.rahul.paymentservice.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private static final long KEY_LIFETIME_HOURS = 24;

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpiredKeys() {
        int deleted = idempotencyKeyRepository.deleteCreatedBefore(LocalDateTime.now().minusHours(KEY_LIFETIME_HOURS));
        if (deleted > 0) {
            log.info("Deleted {} expired idempotency keys", deleted);
        }
    }
}