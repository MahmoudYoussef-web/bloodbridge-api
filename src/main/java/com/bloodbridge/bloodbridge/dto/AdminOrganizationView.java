package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.entity.Organization;

import java.time.LocalDateTime;

/** Flat admin projection of an organization: no lazy collections. */
public record AdminOrganizationView(
        Long id,
        Long userId,
        String orgName,
        String slug,
        String contactEmail,
        String contactPhone,
        String licenseNumber,
        Integer approvalStatus,
        Long governorateId,
        Integer dailyCapacity,
        LocalDateTime createdAt
) {
    public static AdminOrganizationView of(Organization org) {
        return new AdminOrganizationView(
                org.getId(),
                org.getUserId(),
                org.getOrgName(),
                org.getSlug(),
                org.getContactEmail(),
                org.getContactPhone(),
                org.getLicenseNumber(),
                org.getApprovalStatus() != null ? org.getApprovalStatus().getValue() : null,
                org.getGovernorateId(),
                org.getDailyCapacity(),
                org.getCreatedAt()
        );
    }
}
