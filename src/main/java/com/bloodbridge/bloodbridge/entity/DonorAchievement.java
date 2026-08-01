package com.bloodbridge.bloodbridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "donor_achievements", indexes = {
    @Index(name = "idx_da_donor_id", columnList = "donor_id"),
    @Index(name = "idx_da_achievement_id", columnList = "achievement_id"),
    @Index(name = "idx_da_earned_at", columnList = "earned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonorAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "meta", columnDefinition = "JSON")
    private String meta;

    @Column(name = "awarded_by")
    private Long awardedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", insertable = false, updatable = false)
    private Donor donor;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false)
    private Achievement achievement;
}