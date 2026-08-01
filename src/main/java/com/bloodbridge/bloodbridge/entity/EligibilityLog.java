package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "eligibility_logs", indexes = {
    @Index(name = "idx_el_donor_id", columnList = "donor_id"),
    @Index(name = "idx_el_check_type", columnList = "check_type"),
    @Index(name = "idx_el_is_eligible", columnList = "is_eligible"),
    @Index(name = "idx_el_is_permanent", columnList = "is_permanent"),
    @Index(name = "idx_el_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "check_type", nullable = false)
    private Integer checkType;

    @Column(name = "is_eligible", nullable = false)
    private Boolean isEligible;

    @Column(name = "is_permanent", nullable = false)
    @Builder.Default
    private Boolean isPermanent = false;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "answers_snapshot", columnDefinition = "JSON")
    private String answersSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", insertable = false, updatable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private Organization organization;
}