package com.rahul.cinemaservice.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventCleanupJob {

    private static final long RETENTION_DAYS = 7;

    private final ProcessedEventRepository processedEventRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldRecords() {
        int deleted = processedEventRepository.deleteProcessedBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
        if (deleted > 0) {
            log.info("Deleted {} old processed event records", deleted);
        }
    }
}