package com.bloodbridge.bloodbridge.entity;

import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_responses", indexes = {
    @Index(name = "idx_rr_blood_request_id", columnList = "blood_request_id"),
    @Index(name = "idx_rr_donor_id", columnList = "donor_id"),
    @Index(name = "idx_rr_status", columnList = "status"),
    @Index(name = "idx_rr_verification_qr_code", columnList = "verification_qr_code", unique = true),
    @Index(name = "idx_rr_qr_code_expires_at", columnList = "qr_code_expires_at"),
    @Index(name = "idx_rr_responded_at", columnList = "responded_at"),
    @Index(name = "idx_rr_verified_at", columnList = "verified_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blood_request_id", nullable = false)
    private Long bloodRequestId;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.RequestResponseStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequestResponseStatus status = RequestResponseStatus.PENDING;

    @Column(name = "verification_qr_code", length = 64)
    private String verificationQrCode;

    @Column(name = "qr_code_expires_at")
    private LocalDateTime qrCodeExpiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "distance")
    private Float distance;

    @Column(name = "correction_used_at")
    private LocalDateTime correctionUsedAt;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "appointment_id")
    private Long appointmentId;

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
    @JoinColumn(name = "blood_request_id", insertable = false, updatable = false)
    private BloodRequest bloodRequest;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", insertable = false, updatable = false)
    private Donor donor;

    public String getQrStateLabel() {
        if (verificationQrCode == null || verificationQrCode.isBlank()) {
            return "Not available";
        }
        if (verifiedAt != null) {
            return "Used";
        }
        if (qrCodeExpiresAt != null && LocalDateTime.now().isAfter(qrCodeExpiresAt)) {
            return "Expired";
        }
        return "Active";
    }

    public String getQrStateColor() {
        if (verificationQrCode == null || verificationQrCode.isBlank()) {
            return "gray";
        }
        if (verifiedAt != null) {
            return "success";
        }
        if (qrCodeExpiresAt != null && LocalDateTime.now().isAfter(qrCodeExpiresAt)) {
            return "danger";
        }
        return "warning";
    }
}