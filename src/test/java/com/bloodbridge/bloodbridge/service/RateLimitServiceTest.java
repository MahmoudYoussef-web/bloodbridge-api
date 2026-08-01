package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.config.InMemoryRateLimiter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void shouldAllowRequestsWithinLimit() {
        RateLimitService service = new RateLimitService();
        for (int i = 0; i < 30; i++) {
            assertThat(service.tryQrScan("org-1")).isTrue();
        }
    }

    @Test
    void shouldRejectRequestsBeyondLimit() {
        RateLimitService service = new RateLimitService();
        for (int i = 0; i < 30; i++) {
            service.tryQrScan("org-1");
        }
        assertThat(service.tryQrScan("org-1")).isFalse();
    }

    @Test
    void shouldTrackSeparateBucketsPerOrg() {
        RateLimitService service = new RateLimitService();
        for (int i = 0; i < 30; i++) {
            service.tryQrScan("org-1");
        }
        assertThat(service.tryQrScan("org-2")).isTrue();
        assertThat(service.tryQrScan("org-1")).isFalse();
    }

    @Test
    void shouldHandleSequentialKeys() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2, 60);
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void shouldReportRemainingTokens() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(5, 60);
        assertThat(limiter.getRemaining("key")).isEqualTo(5);
        limiter.tryAcquire("key");
        assertThat(limiter.getRemaining("key")).isEqualTo(4);
    }
}
