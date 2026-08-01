package com.bloodbridge.bloodbridge.schedule;

import com.bloodbridge.bloodbridge.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxService outboxService;

    @Scheduled(fixedRate = 30000)
    public void processOutboxEvents() {
        log.debug("Processing outbox events...");
        outboxService.processPendingEvents();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredEvents() {
        log.info("Running daily outbox cleanup...");
    }
}
