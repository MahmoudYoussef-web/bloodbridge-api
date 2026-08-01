-- Seed governorates
INSERT INTO governorates (name_en, name_ar) VALUES
('North Gaza', 'شمال غزة'),
('Gaza', 'غزة'),
('Deir al-Balah', 'دير البلح'),
('Khan Yunis', 'خان يونس'),
('Rafah', 'رفح');

-- Seed basic achievements
INSERT INTO achievements (name, description, points_rewards, badge_icon, badge_type, criteria_type, criteria_value, display_order) VALUES
('{"en": "First Donation", "ar": "أول تبرع"}', '{"en": "Complete your first donation", "ar": "أكمل تبرعك الأول"}', 10, 'badge-first', 'milestone', 'donations_count', 1, 1),
('{"en": "Regular Donor", "ar": "متبرع منتظم"}', '{"en": "Complete 5 donations", "ar": "أكمل 5 تبرعات"}', 50, 'badge-regular', 'milestone', 'donations_count', 5, 2),
('{"en": "Diamond Donor", "ar": "متبرع ماسي"}', '{"en": "Complete 10 donations", "ar": "أكمل 10 تبرعات"}', 100, 'badge-diamond', 'milestone', 'donations_count', 10, 3),
('{"en": "Lifesaver", "ar": "منقذ حياة"}', '{"en": "Complete 25 donations", "ar": "أكمل 25 تبرعة"}', 250, 'badge-lifesaver', 'milestone', 'donations_count', 25, 4),
('{"en": "Legend", "ar": "أسطورة"}', '{"en": "Complete 50 donations", "ar": "أكمل 50 تبرعة"}', 500, 'badge-legend', 'milestone', 'donations_count', 50, 5),
('{"en": "First Responder", "ar": "مستجيب أول"}', '{"en": "Respond to your first blood request", "ar": "استجب لطلب التبرع الأول"}', 5, 'badge-responder', 'behavior', 'first_response', 1, 6);

-- Default scoring settings
INSERT INTO settings (group_name, name, payload, created_at, updated_at) VALUES
('scoring', 'ml_scoring_enabled', '{"value": true}', NOW(), NOW()),
('scoring', 'exploration_ratio', '{"value": 0.2}', NOW(), NOW()),
('scoring', 'max_notifications_per_broadcast', '{"value": 50}', NOW(), NOW()),
('scoring', 'score_staleness_days', '{"value": 30}', NOW(), NOW()),
('scoring', 'min_history_for_exploitation', '{"value": 1}', NOW(), NOW()),
('scoring', 'circuit_breaker_failure_threshold', '{"value": 3}', NOW(), NOW()),
('scoring', 'circuit_breaker_recovery_seconds', '{"value": 120}', NOW(), NOW());

-- Default general settings
INSERT INTO settings (group_name, name, payload, created_at, updated_at) VALUES
('general', 'site_name', '{"value": {"ar": "بلود بريدج", "en": "BloodBridge"}}', NOW(), NOW()),
('general', 'support_email', '{"value": "info@bloodbridge.com"}', NOW(), NOW()),
('general', 'support_phone', '{"value": "+970-59-123-4567"}', NOW(), NOW()),
('general', 'min_donor_age', '{"value": 18}', NOW(), NOW()),
('general', 'max_donor_age', '{"value": 65}', NOW(), NOW()),
('general', 'min_donor_weight', '{"value": 50}', NOW(), NOW()),
('general', 'min_days_between_donations', '{"value": 90}', NOW(), NOW()),
('general', 'min_donor_height', '{"value": 140}', NOW(), NOW()),
('general', 'min_days_after_surgery', '{"value": 28}', NOW(), NOW()),
('general', 'org_max_requests_per_day', '{"value": 5}', NOW(), NOW()),
('general', 'map_default_lat', '{"value": 31.5}', NOW(), NOW()),
('general', 'map_default_lng', '{"value": 34.4667}', NOW(), NOW());
