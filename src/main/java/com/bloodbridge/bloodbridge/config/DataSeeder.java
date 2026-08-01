package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.entity.Achievement;
import com.bloodbridge.bloodbridge.entity.Governorate;
import com.bloodbridge.bloodbridge.entity.Setting;
import com.bloodbridge.bloodbridge.repository.AchievementRepository;
import com.bloodbridge.bloodbridge.repository.GovernorateRepository;
import com.bloodbridge.bloodbridge.repository.SettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("h2")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final GovernorateRepository governorateRepository;
    private final AchievementRepository achievementRepository;
    private final SettingRepository settingRepository;

    @PostConstruct
    public void seed() {
        if (governorateRepository.count() > 0) {
            log.info("Data already seeded, skipping");
            return;
        }

        log.info("Seeding initial data for H2 profile...");

        seedGovernorates();
        seedAchievements();
        seedSettings();

        log.info("Data seeding complete");
    }

    private void seedGovernorates() {
        governorateRepository.save(createGovernorate("North Gaza", "شمال غزة", 31.55, 34.5));
        governorateRepository.save(createGovernorate("Gaza", "غزة", 31.5, 34.4667));
        governorateRepository.save(createGovernorate("Deir al-Balah", "دير البلح", 31.4167, 34.35));
        governorateRepository.save(createGovernorate("Khan Yunis", "خان يونس", 31.3333, 34.3));
        governorateRepository.save(createGovernorate("Rafah", "رفح", 31.2833, 34.25));
        log.info("Seeded 5 governorates");
    }

    private Governorate createGovernorate(String nameEn, String nameAr, double lat, double lng) {
        Governorate g = new Governorate();
        g.setNameEn(nameEn);
        g.setNameAr(nameAr);
        g.setLat(lat);
        g.setLng(lng);
        g.setIsActive(true);
        return g;
    }

    private void seedAchievements() {
        achievementRepository.save(createAchievement("{\"en\": \"First Donation\", \"ar\": \"أول تبرع\"}",
                "{\"en\": \"Complete your first donation\", \"ar\": \"أكمل تبرعك الأول\"}",
                10, "badge-first", "milestone", "donations_count", 1, 1));
        achievementRepository.save(createAchievement("{\"en\": \"Regular Donor\", \"ar\": \"متبرع منتظم\"}",
                "{\"en\": \"Complete 5 donations\", \"ar\": \"أكمل 5 تبرعات\"}",
                50, "badge-regular", "milestone", "donations_count", 5, 2));
        achievementRepository.save(createAchievement("{\"en\": \"Diamond Donor\", \"ar\": \"متبرع ماسي\"}",
                "{\"en\": \"Complete 10 donations\", \"ar\": \"أكمل 10 تبرعات\"}",
                100, "badge-diamond", "milestone", "donations_count", 10, 3));
        achievementRepository.save(createAchievement("{\"en\": \"Lifesaver\", \"ar\": \"منقذ حياة\"}",
                "{\"en\": \"Complete 25 donations\", \"ar\": \"أكمل 25 تبرعة\"}",
                250, "badge-lifesaver", "milestone", "donations_count", 25, 4));
        achievementRepository.save(createAchievement("{\"en\": \"Legend\", \"ar\": \"أسطورة\"}",
                "{\"en\": \"Complete 50 donations\", \"ar\": \"أكمل 50 تبرعة\"}",
                500, "badge-legend", "milestone", "donations_count", 50, 5));
        achievementRepository.save(createAchievement("{\"en\": \"First Responder\", \"ar\": \"مستجيب أول\"}",
                "{\"en\": \"Respond to your first blood request\", \"ar\": \"استجب لطلب التبرع الأول\"}",
                5, "badge-responder", "behavior", "first_response", 1, 6));
        log.info("Seeded 6 achievements");
    }

    private Achievement createAchievement(String name, String description, int pointsRewards,
                                           String badgeIcon, String badgeType,
                                           String criteriaType, int criteriaValue, int displayOrder) {
        Achievement a = new Achievement();
        a.setName(name);
        a.setDescription(description);
        a.setPointsRewards(pointsRewards);
        a.setBadgeIcon(badgeIcon);
        a.setBadgeType(badgeType);
        a.setCriteriaType(criteriaType);
        a.setCriteriaValue(criteriaValue);
        a.setDisplayOrder(displayOrder);
        return a;
    }

    private void seedSettings() {
        seedSetting("scoring", "ml_scoring_enabled", "{\"value\": true}");
        seedSetting("scoring", "exploration_ratio", "{\"value\": 0.2}");
        seedSetting("scoring", "max_notifications_per_broadcast", "{\"value\": 50}");
        seedSetting("scoring", "score_staleness_days", "{\"value\": 30}");
        seedSetting("scoring", "min_history_for_exploitation", "{\"value\": 1}");
        seedSetting("scoring", "circuit_breaker_failure_threshold", "{\"value\": 3}");
        seedSetting("scoring", "circuit_breaker_recovery_seconds", "{\"value\": 120}");
        seedSetting("general", "site_name", "{\"value\": {\"ar\": \"بلود بريدج\", \"en\": \"BloodBridge\"}}");
        seedSetting("general", "support_email", "{\"value\": \"info@bloodbridge.com\"}");
        seedSetting("general", "support_phone", "{\"value\": \"+970-59-123-4567\"}");
        seedSetting("general", "min_donor_age", "{\"value\": 18}");
        seedSetting("general", "max_donor_age", "{\"value\": 65}");
        seedSetting("general", "min_donor_weight", "{\"value\": 50}");
        seedSetting("general", "min_days_between_donations", "{\"value\": 90}");
        seedSetting("general", "min_donor_height", "{\"value\": 140}");
        seedSetting("general", "min_days_after_surgery", "{\"value\": 28}");
        seedSetting("general", "org_max_requests_per_day", "{\"value\": 5}");
        seedSetting("general", "map_default_lat", "{\"value\": 31.5}");
        seedSetting("general", "map_default_lng", "{\"value\": 34.4667}");
        log.info("Seeded 19 settings");
    }

    private void seedSetting(String group, String name, String payload) {
        Setting s = new Setting();
        s.setGroupName(group);
        s.setName(name);
        s.setPayload(payload);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(s);
    }
}
