-- V1__Base_schema.sql
-- Initial schema mirroring Laravel BloodBridge database structure

CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role TINYINT UNSIGNED NOT NULL DEFAULT 1 INDEX,
    locale VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1 INDEX,
    email_verified_at TIMESTAMP NULL,
    phone_verified_at TIMESTAMP NULL,
    remember_token VARCHAR(100) NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_users_email_phone (email, phone),
    INDEX idx_users_role_active (role, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    email VARCHAR(255) PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL INDEX,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    payload LONGTEXT NOT NULL,
    last_activity INT NOT NULL INDEX,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cache (
    `key` VARCHAR(255) PRIMARY KEY,
    value MEDIUMTEXT NOT NULL,
    expiration INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cache_locks (
    `key` VARCHAR(255) PRIMARY KEY,
    owner VARCHAR(255) NOT NULL,
    expiration INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE jobs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    queue VARCHAR(255) NOT NULL INDEX,
    payload LONGTEXT NOT NULL,
    attempts TINYINT UNSIGNED NOT NULL,
    reserved_at INT UNSIGNED NULL,
    available_at INT UNSIGNED NOT NULL,
    created_at INT UNSIGNED NOT NULL,
    INDEX idx_jobs_queue_reserved (queue, reserved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE job_batches (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    total_jobs INT NOT NULL,
    pending_jobs INT NOT NULL,
    failed_jobs INT NOT NULL,
    failed_job_ids LONGTEXT NOT NULL,
    options MEDIUMTEXT NULL,
    cancelled_at INT NULL,
    created_at INT NOT NULL,
    finished_at INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE governorates (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE donors (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    governorate_id BIGINT UNSIGNED NULL,
    national_id VARCHAR(9) NOT NULL UNIQUE INDEX,
    gender VARCHAR(20) NOT NULL INDEX,
    birth_date DATE NULL INDEX,
    auto_location_address VARCHAR(500) NULL,
    lat DECIMAL(10, 7) NULL,
    lng DECIMAL(10, 7) NULL,
    points INT UNSIGNED NOT NULL DEFAULT 0,
    `level` INT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_donors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_donors_governorate FOREIGN KEY (governorate_id) REFERENCES governorates(id) ON DELETE SET NULL,
    INDEX idx_donors_user_national (user_id, national_id),
    INDEX idx_donors_location (lat, lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organizations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    governorate_id BIGINT UNSIGNED NULL,
    org_name JSON NOT NULL,
    slug VARCHAR(255) NULL UNIQUE,
    description JSON NULL,
    license_number VARCHAR(255) NOT NULL UNIQUE INDEX,
    license_document_path VARCHAR(255) NULL,
    responsible_person_name VARCHAR(255) NOT NULL,
    responsible_person_position JSON NULL,
    responsible_person_email VARCHAR(255) NULL,
    contact_email VARCHAR(255) NOT NULL UNIQUE,
    contact_phone VARCHAR(255) NOT NULL UNIQUE,
    auto_location_address VARCHAR(500) NULL,
    lat DECIMAL(10, 7) NULL,
    lng DECIMAL(10, 7) NULL,
    opening_time TIME NULL,
    closing_time TIME NULL,
    working_days JSON NULL,
    daily_capacity INT UNSIGNED NOT NULL DEFAULT 50,
    approved_at TIMESTAMP NULL,
    rejection_reason JSON NULL,
    total_request_created INT UNSIGNED NOT NULL DEFAULT 0,
    total_donation_verified INT UNSIGNED NOT NULL DEFAULT 0,
    approval_status TINYINT UNSIGNED NOT NULL DEFAULT 0 INDEX,
    approved_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_organizations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_organizations_governorate FOREIGN KEY (governorate_id) REFERENCES governorates(id) ON DELETE SET NULL,
    CONSTRAINT fk_organizations_approved_by FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_organizations_location (lat, lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE donor_health_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT UNSIGNED NOT NULL,
    weight INT UNSIGNED NOT NULL COMMENT 'Weight in kg',
    height INT UNSIGNED NOT NULL COMMENT 'Height in cm',
    chronic_disease TINYINT(1) NOT NULL DEFAULT 0,
    recent_donation TINYINT(1) NOT NULL DEFAULT 0,
    infection TINYINT(1) NOT NULL DEFAULT 0,
    is_eligible TINYINT(1) NOT NULL DEFAULT 1 INDEX,
    has_recent_surgery TINYINT(1) NOT NULL DEFAULT 0,
    surgery_date DATE NULL,
    next_eligible_date DATE NULL INDEX,
    last_donation_date DATE NULL INDEX,
    blood_type TINYINT UNSIGNED NULL INDEX,
    verified_by_organization_id BIGINT UNSIGNED NULL,
    verified_at TIMESTAMP NULL,
    verified_blood_type TINYINT UNSIGNED NULL,
    total_donations INT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_health_profiles_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_profiles_verified_by FOREIGN KEY (verified_by_organization_id) REFERENCES organizations(id) ON DELETE SET NULL,
    INDEX idx_health_profile_donor_eligibility (donor_id, is_eligible),
    INDEX idx_health_profile_eligibility_date (next_eligible_date, is_eligible)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blood_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    blood_type TINYINT UNSIGNED NOT NULL INDEX,
    units_needed INT UNSIGNED NOT NULL,
    urgency_level TINYINT UNSIGNED NOT NULL DEFAULT 0 INDEX,
    additional_notes JSON NULL,
    search_radius_km INT UNSIGNED NOT NULL DEFAULT 10,
    actual_search_radius_km INT NULL COMMENT 'Final radius used after expansion',
    lat DECIMAL(10, 7) NULL,
    lng DECIMAL(10, 7) NULL,
    location_address VARCHAR(255) NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 INDEX,
    broadcasted_at TIMESTAMP NULL,
    fulfilled_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_blood_requests_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_blood_requests_location (lat, lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE appointments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    donor_id BIGINT UNSIGNED NOT NULL,
    blood_request_id BIGINT UNSIGNED NULL,
    appointment_date DATETIME NOT NULL INDEX,
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 INDEX,
    cancellation_reason JSON NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_appointments_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_blood_request FOREIGN KEY (blood_request_id) REFERENCES blood_requests(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE request_responses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    blood_request_id BIGINT UNSIGNED NOT NULL,
    donor_id BIGINT UNSIGNED NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 INDEX,
    responded_at TIMESTAMP NULL,
    decline_reason JSON NULL,
    verification_qr_code VARCHAR(255) NULL UNIQUE,
    qr_code_expires_at TIMESTAMP NULL,
    verified_at TIMESTAMP NULL,
    appointment_id BIGINT UNSIGNED NULL,
    correction_used_at TIMESTAMP NULL,
    lat DECIMAL(10, 7) NULL,
    lng DECIMAL(10, 7) NULL,
    distance FLOAT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_responses_blood_request FOREIGN KEY (blood_request_id) REFERENCES blood_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_responses_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    CONSTRAINT fk_responses_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    UNIQUE INDEX idx_responses_donor_request (donor_id, blood_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE eligibility_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT UNSIGNED NOT NULL,
    organization_id BIGINT UNSIGNED NULL,
    check_type TINYINT UNSIGNED NOT NULL INDEX,
    is_eligible TINYINT(1) NOT NULL DEFAULT 1 INDEX,
    is_permanent TINYINT(1) NOT NULL DEFAULT 0,
    rejection_reason TEXT NULL,
    answers_snapshot JSON NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_eligibility_logs_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    CONSTRAINT fk_eligibility_logs_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE donor_predictive_scores (
    donor_id BIGINT UNSIGNED PRIMARY KEY,
    acceptance_probability FLOAT NOT NULL DEFAULT 0.5 COMMENT 'Score from 0.0 to 1.0',
    data_points_count INT NOT NULL DEFAULT 0,
    computed_at TIMESTAMP NULL,
    model_version VARCHAR(10) NOT NULL DEFAULT 'v1.0',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_predictive_scores_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    INDEX idx_predictive_scores_computed_at (computed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE model_training_logs (
    model_version VARCHAR(10) PRIMARY KEY,
    training_date TIMESTAMP NOT NULL,
    data_records_used INT NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'xgboost',
    hyperparameters JSON NULL,
    metrics JSON NULL,
    feature_importance JSON NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_training_logs_date (training_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE achievements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name JSON NOT NULL,
    description JSON NULL,
    points_rewards INT NOT NULL DEFAULT 0,
    badge_icon VARCHAR(255) NULL,
    badge_type VARCHAR(255) NULL INDEX,
    criteria_type VARCHAR(255) NOT NULL,
    criteria_value INT NOT NULL DEFAULT 0,
    display_order INT NOT NULL DEFAULT 0 INDEX,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE donor_achievements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT UNSIGNED NOT NULL,
    achievement_id BIGINT UNSIGNED NOT NULL,
    earned_at TIMESTAMP NULL INDEX,
    meta JSON NULL,
    awarded_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_donor_achievements_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    CONSTRAINT fk_donor_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    CONSTRAINT fk_donor_achievements_awarded_by FOREIGN KEY (awarded_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE INDEX idx_donor_achievement (donor_id, achievement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contact_messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name JSON NOT NULL,
    email JSON NOT NULL,
    subject JSON NOT NULL,
    message JSON NOT NULL,
    ip_address VARCHAR(255) NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE announcements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title JSON NOT NULL,
    body JSON NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_role VARCHAR(255) NULL,
    targeted_users_ids JSON NULL,
    send_via_email TINYINT(1) NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0: draft, 1: published',
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE settings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    locked TINYINT(1) NOT NULL DEFAULT 0,
    payload JSON NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_settings_group_name (group_name, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id CHAR(36) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    notifiable_type VARCHAR(255) NOT NULL,
    notifiable_id BIGINT UNSIGNED NOT NULL,
    data JSON NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notifications_notifiable (notifiable_type, notifiable_id),
    INDEX idx_notifications_read (notifiable_type, notifiable_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
