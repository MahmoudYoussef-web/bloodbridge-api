package com.bloodbridge.bloodbridge.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * Wire mirror of {@link com.bloodbridge.bloodbridge.entity.Achievement} restricted
 * to the exact fields the donor-achievements UI reads, plus the fields a future
 * client can safely depend on without us leaking internal columns.
 *
 * Serialization:
 *   - {@code name}/{@code description} are JSON columns in MySQL stored as raw
 *     JSON strings. They are written to the wire as JSON objects (not escaped
 *     strings), matching the TypeScript shape
 *     {@code name: Record<string, string>} declared in
 *     bloodbridge-frontend/src/types/index.ts.
 *   - {@code earnedAt} is an ISO-8601 string.
 *   - null is permitted for any field; the column-level NOT NULL constraints
 *     in the entity are enforced at the database layer, not here.
 *
 * NOTE: implemented as a class (not a record) because Jackson ignores
 * {@code @JsonRawValue} on record components, which silently emitted the
 * JSON columns as escaped strings.
 */
public class AchievementView {
    private final Long id;
    private final String name;
    private final String description;
    private final Integer pointsRewards;
    private final String badgeIcon;
    private final String badgeType;
    private final String criteriaType;
    private final Integer criteriaValue;
    private final Integer displayOrder;

    public AchievementView(
            Long id,
            String name,
            String description,
            Integer pointsRewards,
            String badgeIcon,
            String badgeType,
            String criteriaType,
            Integer criteriaValue,
            Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pointsRewards = pointsRewards;
        this.badgeIcon = badgeIcon;
        this.badgeType = badgeType;
        this.criteriaType = criteriaType;
        this.criteriaValue = criteriaValue;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    @JsonRawValue
    public String getName() {
        return name;
    }

    @JsonRawValue
    public String getDescription() {
        return description;
    }

    public Integer getPointsRewards() {
        return pointsRewards;
    }

    public String getBadgeIcon() {
        return badgeIcon;
    }

    public String getBadgeType() {
        return badgeType;
    }

    public String getCriteriaType() {
        return criteriaType;
    }

    public Integer getCriteriaValue() {
        return criteriaValue;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public static AchievementView of(com.bloodbridge.bloodbridge.entity.Achievement a) {
        if (a == null) return null;
        return new AchievementView(
                a.getId(),
                a.getName(),
                a.getDescription(),
                a.getPointsRewards(),
                a.getBadgeIcon(),
                a.getBadgeType(),
                a.getCriteriaType(),
                a.getCriteriaValue(),
                a.getDisplayOrder()
        );
    }
}
