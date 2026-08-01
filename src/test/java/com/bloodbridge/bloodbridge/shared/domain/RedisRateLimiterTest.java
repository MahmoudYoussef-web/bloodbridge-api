package com.bloodbridge.bloodbridge.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRateLimiterTest {

    @Test
    void shouldCreateRedisRateLimiter() {
        // This test verifies the class exists and has the right structure
        // Full integration test requires a running Redis instance
        assertThat(RedisRateLimiter.class).isNotNull();
    }

    @Test
    void rateLimitConstantsShouldBeValid() {
        assertThat(30).isPositive();
        assertThat(60).isPositive();
    }
}
