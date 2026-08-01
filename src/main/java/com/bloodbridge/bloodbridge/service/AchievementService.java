package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.entity.Achievement;
import com.bloodbridge.bloodbridge.entity.DonorAchievement;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.repository.AchievementRepository;
import com.bloodbridge.bloodbridge.repository.DonorAchievementRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final DonorAchievementRepository donorAchievementRepository;
    private final RequestResponseRepository requestResponseRepository;

    @Transactional
    public List<DonorAchievement> evaluateAndAward(Long donorId, Long userId) {
        List<Achievement> allAchievements = achievementRepository.findAll();
        List<DonorAchievement> newlyAwarded = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            boolean alreadyAwarded = donorAchievementRepository
                    .existsByDonorIdAndAchievementIdAndEarned(donorId, achievement.getId());

            if (alreadyAwarded) {
                continue;
            }

            boolean qualifies = evaluateAchievement(donorId, achievement);

            if (qualifies) {
                Optional<DonorAchievement> existing = donorAchievementRepository
                        .findByAchievementIdAndDonorId(achievement.getId(), donorId);

                DonorAchievement donorAchievement = existing.orElseGet(() -> {
                    DonorAchievement da = new DonorAchievement();
                    da.setAchievementId(achievement.getId());
                    da.setDonorId(donorId);
                    return da;
                });

                donorAchievement.setEarnedAt(LocalDateTime.now());
                donorAchievementRepository.save(donorAchievement);
                newlyAwarded.add(donorAchievement);

                log.info("Achievement '{}' awarded to donor {} (user {})",
                        achievement.getName(), donorId, userId);
            }
        }

        return newlyAwarded;
    }

    private boolean evaluateAchievement(Long donorId, Achievement achievement) {
        String criteriaType = achievement.getCriteriaType() != null
                ? achievement.getCriteriaType().toLowerCase() : "";

        int criteriaValue = achievement.getCriteriaValue() != 0
                ? achievement.getCriteriaValue() : 1;

        List<RequestResponse> completed = requestResponseRepository
                .findByDonorIdAndStatus(donorId, RequestResponseStatus.COMPLETED);
        long completedCount = completed.size();

        if ("donations_count".equals(criteriaType)) {
            return completedCount >= criteriaValue;
        }

        if ("first_response".equals(criteriaType)) {
            return completedCount >= 1;
        }

        log.warn("Unknown achievement criteria '{}' for achievement {}. Evaluating as false.",
                criteriaType, achievement.getId());
        return false;
    }

    public List<Achievement> getAvailableAchievements() {
        return achievementRepository.findAll();
    }

    public List<DonorAchievement> getDonorAchievements(Long donorId) {
        return donorAchievementRepository.findByDonorId(donorId);
    }

    public long getDonorAchievementCount(Long donorId) {
        return donorAchievementRepository.countByDonorIdAndEarnedTrue(donorId);
    }
}