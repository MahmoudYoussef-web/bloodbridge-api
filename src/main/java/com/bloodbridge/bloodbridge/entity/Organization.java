package com.bloodbridge.bloodbridge.entity;

import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "org_name", nullable = false, length = 255)
    private String orgName;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "governorate_id")
    private Long governorateId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "license_document_path", length = 500)
    private String licenseDocumentPath;

    @Column(name = "responsible_person_name", length = 255)
    private String responsiblePersonName;

    @Column(name = "responsible_person_position", length = 100)
    private String responsiblePersonPosition;

    @Column(name = "responsible_person_email", length = 255)
    private String responsiblePersonEmail;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "street_address", length = 500)
    private String streetAddress;

    @Column(name = "auto_location_address", length = 500)
    private String autoLocationAddress;

    @Column
    private Double lat;

    @Column
    private Double lng;

    @Column(name = "opening_time")
    private java.time.LocalTime openingTime;

    @Column(name = "closing_time")
    private java.time.LocalTime closingTime;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "working_days", columnDefinition = "JSON")
    private String workingDays;

    @Column(name = "daily_capacity")
    @Builder.Default
    private Integer dailyCapacity = 0;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.OrganizationStatusConverter.class)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus approvalStatus = OrganizationStatus.PENDING;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "governorate_id", insertable = false, updatable = false)
    private Governorate governorate;

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private java.util.List<BloodRequest> bloodRequests;
    
    public String getLocalizedOrgName(String locale) {
        return orgName;
    }
}