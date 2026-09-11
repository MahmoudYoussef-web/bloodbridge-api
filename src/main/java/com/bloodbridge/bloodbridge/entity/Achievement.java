package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievements", indexes = {
    @Index(name = "idx_ach_badge_type", columnList = "badge_type"),
    @Index(name = "idx_ach_display_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "name", nullable = false, columnDefinition = "JSON")
    @NotBlank(message = "Name is required")
    private String name;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "description", columnDefinition = "JSON")
    private String description;

    @Column(name = "points_rewards")
    @Builder.Default
    @Min(value = 0, message = "Points must be zero or positive")
    private Integer pointsRewards = 0;

    @Column(name = "badge_icon", length = 255)
    private String badgeIcon;

    @Column(name = "badge_type", length = 255)
    private String badgeType;

    @Column(name = "criteria_type", nullable = false, length = 255)
    @NotBlank(message = "Criteria type is required")
    private String criteriaType;

    @Column(name = "criteria_value")
    @Builder.Default
    @Min(value = 0, message = "Criteria value must be zero or positive")
    private Integer criteriaValue = 0;

    @Column(name = "display_order")
    @Builder.Default
    @Min(value = 0, message = "Display order must be zero or positive")
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}