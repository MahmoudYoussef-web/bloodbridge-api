package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appt_blood_request_id", columnList = "blood_request_id"),
    @Index(name = "idx_appt_donor_id", columnList = "donor_id"),
    @Index(name = "idx_appt_organization_id", columnList = "organization_id"),
    @Index(name = "idx_appt_status", columnList = "status"),
    @Index(name = "idx_appt_appointment_time", columnList = "appointment_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blood_request_id", nullable = false)
    private Long bloodRequestId;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "scheduled";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_request_id", insertable = false, updatable = false)
    private BloodRequest bloodRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", insertable = false, updatable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private Organization organization;
}