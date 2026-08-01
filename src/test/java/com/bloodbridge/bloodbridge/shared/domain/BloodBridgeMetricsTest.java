package com.bloodbridge.bloodbridge.shared.domain;

import com.bloodbridge.bloodbridge.shared.monitoring.BloodBridgeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BloodBridgeMetricsTest {

    private BloodBridgeMetrics metrics;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new BloodBridgeMetrics(meterRegistry);
    }

    @Test
    void shouldIncrementBroadcastCounter() {
        metrics.incrementBroadcast();
        double count = meterRegistry.counter("bloodbridge.broadcast.total").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementDonationCounter() {
        metrics.incrementDonationComplete();
        double count = meterRegistry.counter("bloodbridge.donation.completed").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementQrScanCounter() {
        metrics.incrementQrScan();
        double count = meterRegistry.counter("bloodbridge.qr.scans").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementNotificationCounter() {
        metrics.incrementNotificationSent();
        double count = meterRegistry.counter("bloodbridge.notifications.sent").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldRecordTimer() {
        String result = metrics.timeBroadcast(() -> "test");
        assertThat(result).isEqualTo("test");
    }

    @Test
    void shouldRunRunnableWithTimer() {
        metrics.timeBroadcast(() -> {});
    }
}
