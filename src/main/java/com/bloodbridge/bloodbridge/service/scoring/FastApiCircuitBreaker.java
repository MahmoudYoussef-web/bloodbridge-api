package com.bloodbridge.bloodbridge.service.scoring;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiCircuitBreaker {

    private static final String STATE_CLOSED = "closed";
    private static final String STATE_OPEN = "open";
    private static final String STATE_HALF_OPEN = "half_open";

    private final ScoringSettingsService scoringSettings;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile String state = STATE_CLOSED;
    private volatile Instant openedAt = Instant.MIN;

    @Value("${bloodbridge.fastapi.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${bloodbridge.fastapi.read-timeout:8000}")
    private int readTimeout;

    @PostConstruct
    public void init() {
        this.state = STATE_CLOSED;
        this.failureCount.set(0);
    }

    public <T> T attempt(FastApiCallable<T> request) throws Exception {
        if (state.equals(STATE_OPEN)) {
            if (shouldAttemptReset()) {
                transitionTo(STATE_HALF_OPEN);
            } else {
                log.debug("Circuit breaker OPEN - skipping FastAPI call");
                return null;
            }
        }

        try {
            T result = request.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            return null;
        }
    }

    public boolean isAvailable() {
        if (state.equals(STATE_OPEN) && !shouldAttemptReset()) {
            return false;
        }
        return true;
    }

    public String getState() {
        return state;
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    private void transitionTo(String newState) {
        this.state = newState;
        log.info("FastAPI circuit breaker -> {}", newState);
    }

    private boolean shouldAttemptReset() {
        int recoverySeconds = scoringSettings.getCircuitBreakerRecoverySeconds();
        return Duration.between(openedAt, Instant.now()).getSeconds() >= recoverySeconds;
    }

    private void onSuccess() {
        failureCount.set(0);
        openedAt = Instant.MIN;
        transitionTo(STATE_CLOSED);
    }

    private void onFailure(Exception e) {
        int failures = failureCount.incrementAndGet();
        int threshold = scoringSettings.getCircuitBreakerFailureThreshold();

        log.warn("FastAPI failure #{}: {}", failures, e.getMessage());

        if (failures >= threshold) {
            openedAt = Instant.now();
            transitionTo(STATE_OPEN);
            log.error("Circuit breaker OPENED after {} consecutive failures", failures);
        }
    }

    @FunctionalInterface
    public interface FastApiCallable<T> {
        T call() throws Exception;
    }
}