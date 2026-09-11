package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.entity.Achievement;
import com.bloodbridge.bloodbridge.entity.Announcement;
import com.bloodbridge.bloodbridge.entity.Governorate;
import com.bloodbridge.bloodbridge.entity.Setting;
import com.bloodbridge.bloodbridge.repository.AchievementRepository;
import com.bloodbridge.bloodbridge.repository.AnnouncementRepository;
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
    private final AnnouncementRepository announcementRepository;

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
        seedAnnouncements();

        log.info("Data seeding complete");
    }

    private void seedGovernorates() {
        governorateRepository.save(createGovernorate("Cairo", "القاهرة", 30.0444, 31.2357));
        governorateRepository.save(createGovernorate("Giza", "الجيزة", 30.0131, 31.2089));
        governorateRepository.save(createGovernorate("Alexandria", "الإسكندرية", 31.2001, 29.9187));
        governorateRepository.save(createGovernorate("Dakahlia", "الدقهلية", 31.0379, 31.3815));
        governorateRepository.save(createGovernorate("Qalyubia", "القليوبية", 30.4650, 31.1848));
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
        seedSetting("general", "support_phone", "{\"value\": \"+20-100-123-4567\"}");
        seedSetting("general", "min_donor_age", "{\"value\": 18}");
        seedSetting("general", "max_donor_age", "{\"value\": 65}");
        seedSetting("general", "min_donor_weight", "{\"value\": 50}");
        seedSetting("general", "min_days_between_donations", "{\"value\": 90}");
        seedSetting("general", "min_donor_height", "{\"value\": 140}");
        seedSetting("general", "min_days_after_surgery", "{\"value\": 28}");
        seedSetting("general", "org_max_requests_per_day", "{\"value\": 5}");
        seedSetting("general", "map_default_lat", "{\"value\": 30.0444}");
        seedSetting("general", "map_default_lng", "{\"value\": 31.2357}");
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

    private void seedAnnouncements() {
        Announcement regular = Announcement.builder()
                .titleEn("Give blood every 90 days")
                .titleAr("تبرع بالدم كل 90 يومًا")
                .contentEn("Most donors become eligible again 90 days after their last donation. Keep your profile updated and we will page you when your type is needed.")
                .contentAr("يصبح معظم المتبرعين مؤهلين مجددًا بعد 90 يومًا من آخر تبرع. حدّث بياناتك وسنناديك عند الحاجة إلى فصيلتك.")
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .build();
        announcementRepository.save(regular);

        Announcement donorDay = Announcement.builder()
                .titleEn("World Blood Donor Day — June 14")
                .titleAr("اليوم العالمي للمتبرعين بالدم — 14 يونيو")
                .contentEn("A global thank-you to voluntary donors. One donation can save up to three lives — bring a friend to your next appointment.")
                .contentAr("تحية عالمية للمتبرعين. تبرع واحد قد ينقذ حتى ثلاثة أرواح — اصطحب صديقًا إلى موعدك القادم.")
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusMonths(9).withDayOfMonth(14)
                        .withHour(10).withMinute(0).withSecond(0).withNano(0))
                .build();
        announcementRepository.save(donorDay);
        log.info("Seeded 2 announcements");
    }
}
