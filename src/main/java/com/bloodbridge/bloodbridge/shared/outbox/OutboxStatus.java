package com.bloodbridge.bloodbridge.shared.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
