package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.entity.BloodRequest;

import java.time.LocalDateTime;

/** Flat admin projection of a blood request, including the owning org name. */
public record AdminBloodRequestView(
        Long id,
        Long organizationId,
        String organizationName,
        Integer bloodType,
        Integer unitsNeeded,
        Integer urgencyLevel,
        Integer status,
        String locationAddress,
        LocalDateTime broadcastedAt,
        LocalDateTime createdAt
) {
    public static AdminBloodRequestView of(BloodRequest br) {
        return new AdminBloodRequestView(
                br.getId(),
                br.getOrganizationId(),
                br.getOrganization() != null ? br.getOrganization().getOrgName() : null,
                br.getBloodType() != null ? br.getBloodType().getValue() : null,
                br.getUnitsNeeded(),
                br.getUrgencyLevel() != null ? br.getUrgencyLevel().getValue() : null,
                br.getStatus() != null ? br.getStatus().getValue() : null,
                br.getLocationAddress(),
                br.getBroadcastedAt(),
                br.getCreatedAt()
        );
    }
}
