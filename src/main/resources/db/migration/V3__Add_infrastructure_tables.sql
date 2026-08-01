-- =====================================================
-- V3: Infrastructure tables for Outbox, Audit, Idempotency
-- =====================================================

-- =====================================================
-- 1. Outbox Events (Transactional Outbox Pattern)
-- =====================================================
CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    error_message TEXT NULL,
    retry_count INT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_outbox_status (status),
    INDEX idx_outbox_created_at (created_at),
    INDEX idx_outbox_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. Audit Logs
-- =====================================================
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT NULL,
    performed_by_role VARCHAR(50) NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_entity_type (entity_type),
    INDEX idx_audit_entity_id (entity_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_performed_by (performed_by),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. Idempotency Keys
-- =====================================================
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(64) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    response_status INT NULL,
    response_body JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    PRIMARY KEY (idempotency_key),
    INDEX idx_idempotency_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 4. Add version column for optimistic locking
-- =====================================================
ALTER TABLE blood_requests ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE donors ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE request_responses ADD COLUMN version BIGINT DEFAULT 0;
