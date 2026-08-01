package com.bloodbridge.bloodbridge.entity;

import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests", indexes = {
    @Index(name = "idx_br_organization_id", columnList = "organization_id"),
    @Index(name = "idx_br_status", columnList = "status"),
    @Index(name = "idx_br_blood_type", columnList = "blood_type"),
    @Index(name = "idx_br_urgency_level", columnList = "urgency_level"),
    @Index(name = "idx_br_created_at", columnList = "created_at"),
    @Index(name = "idx_br_lat_lng", columnList = "lat,lng"),
    @Index(name = "idx_br_broadcasted_at", columnList = "broadcasted_at"),
    @Index(name = "idx_br_fulfilled_at", columnList = "fulfilled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.BloodTypeConverter.class)
    @Column(name = "blood_type", nullable = false, length = 20)
    private BloodType bloodType;

    @Column(name = "units_needed", nullable = false)
    private Integer unitsNeeded;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.UrgencyLevelConverter.class)
    @Column(name = "urgency_level", nullable = false, length = 20)
    @Builder.Default
    private UrgencyLevel urgencyLevel = UrgencyLevel.NORMAL;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "search_radius_km", nullable = false)
    @Builder.Default
    private Integer searchRadiusKm = 10;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "location_address", columnDefinition = "TEXT")
    private String locationAddress;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.BloodRequestStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BloodRequestStatus status = BloodRequestStatus.PENDING;

    @Column(name = "broadcasted_at")
    private LocalDateTime broadcastedAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @Column(name = "actual_search_radius_km")
    private Integer actualSearchRadiusKm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private Organization organization;

    @JsonIgnore
    @OneToMany(mappedBy = "bloodRequest", fetch = FetchType.LAZY)
    private java.util.List<RequestResponse> responses;

    public boolean isActive() {
        return status == BloodRequestStatus.BROADCASTED && fulfilledAt == null && deletedAt == null;
    }

    public boolean wasExpanded() {
        return actualSearchRadiusKm != null && actualSearchRadiusKm > searchRadiusKm;
    }

    public int getExpansionSteps() {
        if (!wasExpanded()) return 0;
        return (actualSearchRadiusKm - searchRadiusKm) / 5;
    }

    public String getExpansionSummary() {
        if (!wasExpanded()) {
            return "Searched at " + searchRadiusKm + "km";
        }
        int steps = getExpansionSteps();
        return "Expanded from " + searchRadiusKm + "km to " + actualSearchRadiusKm + "km (" + steps + " expansion" + (steps > 1 ? "s" : "") + ")";
    }
}