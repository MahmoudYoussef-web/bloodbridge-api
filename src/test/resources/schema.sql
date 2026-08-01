CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified_at TIMESTAMP NULL,
    phone_verified_at TIMESTAMP NULL,
    remember_token VARCHAR(100),
    locale VARCHAR(5) DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS days_since_donation (
    donor_id BIGINT PRIMARY KEY,
    days_since INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS blood_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    blood_type INT NOT NULL,
    units_needed INT NOT NULL,
    urgency_level INT NOT NULL DEFAULT 0,
    additional_notes TEXT,
    search_radius_km INT NOT NULL DEFAULT 10,
    lat DOUBLE,
    lng DOUBLE,
    actual_search_radius_km INT,
    status INT NOT NULL DEFAULT 0,
    broadcasted_at TIMESTAMP NULL,
    fulfilled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS request_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blood_request_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    status INT NOT NULL DEFAULT 0,
    verification_qr_code VARCHAR(64),
    qr_code_expires_at TIMESTAMP NULL,
    verified_at TIMESTAMP NULL,
    lat DOUBLE,
    lng DOUBLE,
    distance FLOAT,
    correction_used_at TIMESTAMP NULL,
    decline_reason TEXT,
    responded_at TIMESTAMP NULL,
    appointment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS donors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    governorate_id BIGINT,
    national_id VARCHAR(9) UNIQUE,
    gender INT,
    birth_date DATE,
    auto_location_address VARCHAR(500),
    lat DOUBLE,
    lng DOUBLE,
    points INT DEFAULT 0,
    level INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS donor_health_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL UNIQUE,
    weight INT,
    height INT,
    chronic_disease BOOLEAN NOT NULL DEFAULT FALSE,
    recent_donation BOOLEAN NOT NULL DEFAULT FALSE,
    infection BOOLEAN NOT NULL DEFAULT FALSE,
    is_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    has_recent_surgery BOOLEAN NOT NULL DEFAULT FALSE,
    surgery_date DATE,
    next_eligible_date DATE,
    last_donation_date DATE,
    blood_type INT,
    verified_blood_type INT,
    verified_by_organization_id BIGINT,
    verified_at TIMESTAMP NULL,
    total_donations INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    org_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    governorate_id BIGINT,
    description TEXT,
    license_number VARCHAR(255),
    license_document_path VARCHAR(255),
    responsible_person_name VARCHAR(255),
    responsible_person_position VARCHAR(255),
    responsible_person_email VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    street_address VARCHAR(255),
    auto_location_address VARCHAR(500),
    lat DOUBLE,
    lng DOUBLE,
    opening_time TIME,
    closing_time TIME,
    working_days VARCHAR(255),
    daily_capacity INT DEFAULT 0,
    approval_status INT NOT NULL DEFAULT 0,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    notifiable_type VARCHAR(255) NOT NULL,
    notifiable_id BIGINT NOT NULL,
    data JSON,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eligibility_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    is_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    is_permanent BOOLEAN NOT NULL DEFAULT FALSE,
    check_type INT NOT NULL DEFAULT 1,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS governorates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name JSON NOT NULL,
    description JSON NOT NULL,
    points_rewards INT NOT NULL DEFAULT 0,
    badge_icon VARCHAR(255),
    badge_type VARCHAR(50),
    criteria_type VARCHAR(50),
    criteria_value INT NOT NULL DEFAULT 0,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS donor_achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS donor_predictive_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL UNIQUE,
    score DOUBLE NOT NULL DEFAULT 0.5,
    model_version VARCHAR(50),
    scored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'unread',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    payload JSON NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (group_name, name)
);

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

INSERT INTO governorates (name_en, name_ar) VALUES ('Gaza', 'غزة');
INSERT INTO governorates (name_en, name_ar) VALUES ('North Gaza', 'شمال غزة');
INSERT INTO governorates (name_en, name_ar) VALUES ('Deir al-Balah', 'دير البلح');
INSERT INTO governorates (name_en, name_ar) VALUES ('Khan Yunis', 'خان يونس');
INSERT INTO governorates (name_en, name_ar) VALUES ('Rafah', 'رفح');

INSERT INTO achievements (name, description, points_rewards, badge_icon, badge_type, criteria_type, criteria_value, display_order) VALUES
('{"en": "First Donation", "ar": "أول تبرع"}', '{"en": "Complete your first donation", "ar": "أكمل تبرعك الأول"}', 10, 'badge-first', 'milestone', 'donations_count', 1, 1),
('{"en": "Regular Donor", "ar": "متبرع منتظم"}', '{"en": "Complete 5 donations", "ar": "أكمل 5 تبرعات"}', 50, 'badge-regular', 'milestone', 'donations_count', 5, 2);

INSERT INTO settings (group_name, name, payload) VALUES
('scoring', 'ml_scoring_enabled', '{"value": false}'),
('scoring', 'exploration_ratio', '{"value": 0.2}'),
('scoring', 'max_notifications_per_broadcast', '{"value": 50}'),
('scoring', 'score_staleness_days', '{"value": 30}'),
('scoring', 'min_history_for_exploitation', '{"value": 1}'),
('scoring', 'circuit_breaker_failure_threshold', '{"value": 3}'),
('scoring', 'circuit_breaker_recovery_seconds', '{"value": 120}');

INSERT INTO settings (group_name, name, payload) VALUES
('general', 'site_name', '{"value": {"ar": "بلود بريدج", "en": "BloodBridge"}}'),
('general', 'support_email', '{"value": "info@bloodbridge.com"}'),
('general', 'min_donor_weight', '{"value": 50}'),
('general', 'min_donor_height', '{"value": 140}'),
('general', 'min_days_between_donations', '{"value": 90}'),
('general', 'min_days_after_surgery', '{"value": 28}');
