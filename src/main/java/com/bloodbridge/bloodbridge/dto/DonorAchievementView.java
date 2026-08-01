package com.bloodbridge.bloodbridge.dto;

/**
 * Wire mirror of {@link com.bloodbridge.bloodbridge.entity.DonorAchievement}
 * restricted to the exact fields the donor-achievements UI reads:
 *   - id (the row id, used as React key)
 *   - donorId, achievementId (FK columns)
 *   - earnedAt (ISO-8601 timestamp)
 *   - achievement (nested AchievementView)
 *
 * Fields intentionally NOT exposed on the wire:
 *   - meta, awardedBy (operational/admin data, not user-facing)
 *   - createdAt, updatedAt, deletedAt (audit columns, leaked entities problem)
 *   - donor (lazy-load risk + not needed by the UI)
 *
 * The full entity is still loadable inside the service layer for admin flows
 * via {@code donorAchievementRepository.findById}.
 */
public record DonorAchievementView(
        Long id,
        Long donorId,
        Long achievementId,
        String earnedAt,
        AchievementView achievement
) {
    public static DonorAchievementView of(
            com.bloodbridge.bloodbridge.entity.DonorAchievement da,
            AchievementView achievement) {
        if (da == null) return null;
        return new DonorAchievementView(
                da.getId(),
                da.getDonorId(),
                da.getAchievementId(),
                da.getEarnedAt() == null ? null : da.getEarnedAt().toString(),
                achievement
        );
    }
}
