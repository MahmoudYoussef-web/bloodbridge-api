package com.bloodbridge.bloodbridge.entity;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donor_health_profiles", indexes = {
    @Index(name = "idx_dhp_donor_id", columnList = "donor_id", unique = true),
    @Index(name = "idx_dhp_is_eligible", columnList = "is_eligible"),
    @Index(name = "idx_dhp_next_eligible_date", columnList = "next_eligible_date"),
    @Index(name = "idx_dhp_chronic_disease", columnList = "chronic_disease"),
    @Index(name = "idx_dhp_verified_blood_type", columnList = "verified_blood_type"),
    @Index(name = "idx_dhp_blood_type", columnList = "blood_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonorHealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false, unique = true)
    private Donor donor;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "height")
    private Integer height;

    @Column(name = "chronic_disease", nullable = false)
    @Builder.Default
    private Boolean chronicDisease = false;

    @Column(name = "recent_donation", nullable = false)
    @Builder.Default
    private Boolean recentDonation = false;

    @Column(name = "infection", nullable = false)
    @Builder.Default
    private Boolean infection = false;

    @Column(name = "is_eligible", nullable = false)
    @Builder.Default
    private Boolean isEligible = true;

    @Column(name = "has_recent_surgery", nullable = false)
    @Builder.Default
    private Boolean hasRecentSurgery = false;

    @Column(name = "surgery_date")
    private LocalDate surgeryDate;

    @Column(name = "next_eligible_date")
    private LocalDate nextEligibleDate;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.BloodTypeConverter.class)
    @Column(name = "blood_type", length = 20)
    private BloodType bloodType;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.BloodTypeConverter.class)
    @Column(name = "verified_blood_type", length = 20)
    private BloodType verifiedBloodType;

    @Column(name = "verified_by_organization_id")
    private Long verifiedByOrganizationId;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "total_donations", nullable = false)
    @Builder.Default
    private Integer totalDonations = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EligibilityResult calculateEligibility() {
        LocalDate today = LocalDate.now();
        
        LocalDate nextEligibleDate = (this.nextEligibleDate != null && this.nextEligibleDate.isAfter(today))
            ? this.nextEligibleDate
            : null;
        
        boolean isEligible = nextEligibleDate == null;
        
        if (Boolean.TRUE.equals(this.chronicDisease)) {
            return new EligibilityResult(false, null);
        }
        
        if ((this.weight != null && this.weight < 50) || (this.height != null && this.height < 140)) {
            isEligible = false;
        }
        
        if (Boolean.TRUE.equals(this.infection)) {
            isEligible = false;
            LocalDate infectionDate = today.plusDays(14);
            if (nextEligibleDate == null || infectionDate.isAfter(nextEligibleDate)) {
                nextEligibleDate = infectionDate;
            }
        }
        
        if (this.lastDonationDate != null) {
            long daysSince = java.time.temporal.ChronoUnit.DAYS.between(this.lastDonationDate, today);
            if (daysSince < 90) {
                isEligible = false;
                LocalDate donationDate = this.lastDonationDate.plusDays(90);
                if (nextEligibleDate == null || donationDate.isAfter(nextEligibleDate)) {
                    nextEligibleDate = donationDate;
                }
            }
        }
        
        if (this.surgeryDate != null) {
            long daysSince = java.time.temporal.ChronoUnit.DAYS.between(this.surgeryDate, today);
            if (daysSince < 28) {
                isEligible = false;
                LocalDate surgeryEligibleDate = this.surgeryDate.plusDays(28);
                if (nextEligibleDate == null || surgeryEligibleDate.isAfter(nextEligibleDate)) {
                    nextEligibleDate = surgeryEligibleDate;
                }
            }
        }
        
        return new EligibilityResult(isEligible, nextEligibleDate);
    }

    @Getter
    @AllArgsConstructor
    public static class EligibilityResult {
        private final boolean isEligible;
        private final LocalDate nextEligibleDate;
    }
}