package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "donor_predictive_scores", indexes = {
    @Index(name = "idx_dps_donor_id", columnList = "donor_id", unique = true),
    @Index(name = "idx_dps_computed_at", columnList = "computed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonorPredictiveScore {

    @Id
    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "acceptance_probability", nullable = false)
    private Double acceptanceProbability;

    @Column(name = "data_points_count")
    @Builder.Default
    private Integer dataPointsCount = 0;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", insertable = false, updatable = false)
    private Donor donor;
}