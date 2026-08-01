package com.bloodbridge.bloodbridge.schedule;

import com.bloodbridge.bloodbridge.shared.idempotency.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanup {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupExpiredKeys() {
        int deleted = idempotencyKeyRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency keys", deleted);
        }
    }
}
