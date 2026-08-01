package com.bloodbridge.bloodbridge.shared.domain;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(120))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker fastApiBreaker =
                registry.circuitBreaker("fastApiService");
        io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics metrics = fastApiBreaker.getMetrics();

        io.micrometer.core.instrument.Gauge.builder("resilience4j.circuitbreaker.state.fastapi",
                () -> fastApiBreaker.getState().getOrder())
                .description("FastAPI circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
                .register(meterRegistry);

        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        registry.retry("fastApiRetry", config);
        registry.retry("dbRetry", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(100))
                .build());
        return registry;
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(5)
                .maxWaitDuration(Duration.ofMillis(500))
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        registry.bulkhead("notificationBulkhead", config);
        registry.bulkhead("broadcastBulkhead", BulkheadConfig.custom()
                .maxConcurrentCalls(3)
                .maxWaitDuration(Duration.ofMillis(1000))
                .build());
        return registry;
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
        registry.timeLimiter("fastApiTimeLimiter", config);
        return registry;
    }
}
