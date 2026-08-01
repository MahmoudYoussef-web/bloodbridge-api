package com.bloodbridge.bloodbridge.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * Wire mirror for the nested {@code BloodRequest} reached from a
 * {@code RequestResponseView}.  Mirrors the frontend TS shape
 * {@code r.bloodRequest.organization.orgName} exactly.
 *
 * Excluded (not read by any list-screen consumer):
 *   - urgencyLevel, status, searchRadiusKm, additionalNotes, lat, lng,
 *     locationAddress, broadcastedAt, fulfilledAt, actualSearchRadiusKm,
 *     distance, organizationId, responsesCount, donorsAccepted,
 *     donorsCompleted, createdAt, updatedAt, version
 */
public record BloodRequestLiteView(
        Long id,
        Integer bloodType,
        Integer unitsNeeded,
        OrganizationLiteView organization
) {
    public record OrganizationLiteView(String orgName) {}

    public static BloodRequestLiteView of(
            com.bloodbridge.bloodbridge.entity.BloodRequest br,
            String orgName) {
        if (br == null) return null;
        return new BloodRequestLiteView(
                br.getId(),
                br.getBloodType() == null ? null : br.getBloodType().getValue(),
                br.getUnitsNeeded(),
                new OrganizationLiteView(orgName));
    }
}
