package com.bloodbridge.bloodbridge.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record DonorAchievementsResponse(
    List<DonorAchievementView> earned,
    List<AchievementView> locked,
    int points,
    int level
) {}
