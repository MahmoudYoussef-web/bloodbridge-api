package com.bloodbridge.bloodbridge.shared.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class BloodBridgeMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter broadcastCounter;
    private final Counter donationCompleteCounter;
    private final Counter qrScanCounter;
    private final Counter notificationSentCounter;
    private final Counter rateLimitExceededCounter;
    private final Counter idempotencyReplayCounter;
    private final Counter aiScoringCounter;
    private final Counter aiScoringFailureCounter;

    private final Timer broadcastTimer;
    private final Timer notificationTimer;
    private final Timer aiResponseTimer;
    private final Timer dbQueryTimer;

    public BloodBridgeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.broadcastCounter = Counter.builder("bloodbridge.broadcast.total")
                .description("Total blood request broadcasts")
                .register(meterRegistry);

        this.donationCompleteCounter = Counter.builder("bloodbridge.donation.completed")
                .description("Total completed donations")
                .register(meterRegistry);

        this.qrScanCounter = Counter.builder("bloodbridge.qr.scans")
                .description("Total QR code scans")
                .register(meterRegistry);

        this.notificationSentCounter = Counter.builder("bloodbridge.notifications.sent")
                .description("Total notifications sent")
                .register(meterRegistry);

        this.rateLimitExceededCounter = Counter.builder("bloodbridge.ratelimit.exceeded")
                .description("Rate limit exceeded count")
                .register(meterRegistry);

        this.idempotencyReplayCounter = Counter.builder("bloodbridge.idempotency.replay")
                .description("Idempotency key replays")
                .register(meterRegistry);

        this.aiScoringCounter = Counter.builder("bloodbridge.ai.scoring.total")
                .description("Total AI scoring calls")
                .register(meterRegistry);

        this.aiScoringFailureCounter = Counter.builder("bloodbridge.ai.scoring.failures")
                .description("AI scoring failure count")
                .register(meterRegistry);

        this.broadcastTimer = Timer.builder("bloodbridge.broadcast.duration")
                .description("Broadcast execution time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.notificationTimer = Timer.builder("bloodbridge.notification.duration")
                .description("Notification dispatch time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.aiResponseTimer = Timer.builder("bloodbridge.ai.response.time")
                .description("AI service response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.dbQueryTimer = Timer.builder("bloodbridge.db.query.time")
                .description("Database query execution time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void incrementBroadcast() { broadcastCounter.increment(); }
    public void incrementDonationComplete() { donationCompleteCounter.increment(); }
    public void incrementQrScan() { qrScanCounter.increment(); }
    public void incrementNotificationSent() { notificationSentCounter.increment(); }
    public void incrementRateLimitExceeded() { rateLimitExceededCounter.increment(); }
    public void incrementIdempotencyReplay() { idempotencyReplayCounter.increment(); }
    public void incrementAiScoring() { aiScoringCounter.increment(); }
    public void incrementAiScoringFailure() { aiScoringFailureCounter.increment(); }

    public void recordActiveRequests(long count) {
        meterRegistry.gauge("bloodbridge.requests.active", count);
    }

    public void recordDonorCount(long count) {
        meterRegistry.gauge("bloodbridge.donors.total", count);
    }

    public <T> T timeBroadcast(Supplier<T> supplier) {
        return broadcastTimer.record(supplier);
    }

    public void timeBroadcast(Runnable runnable) {
        broadcastTimer.record(runnable);
    }

    public <T> T timeNotification(Supplier<T> supplier) {
        return notificationTimer.record(supplier);
    }

    public <T> T timeAiResponse(Supplier<T> supplier) {
        return aiResponseTimer.record(supplier);
    }

    public <T> T timeDbQuery(Supplier<T> supplier) {
        return dbQueryTimer.record(supplier);
    }

    public void recordTimer(String name, long durationMs) {
        Timer.builder(name)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
