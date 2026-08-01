package com.bloodbridge.bloodbridge.dto;

/**
 * Wire mirror of {@link com.bloodbridge.bloodbridge.entity.RequestResponse}
 * restricted to the exact fields the donor/org list screens read.
 *
 * Replaces the raw {@code RequestResponse} entity in:
 *   - GET /donor/responses          (DonorHistory, DonorDashboard)
 *   - GET /org/responses            (OrgDashboard, OrgStatistics)
 *
 * NOT YET wired into (no live consumers today):
 *   - GET /org/blood-requests/{id}/responses
 *   - GET /admin/responses
 * Those endpoints still return raw {@code RequestResponse} entities.
 *
 * Excluded (no list-screen reads them):
 *   - donorId, verificationQrCode, qrCodeExpiresAt,
 *     declineReason, distance, updatedAt
 *   - All BloodRequest fields except bloodType, unitsNeeded, organization.orgName
 *   - All Donor/User fields except donor.user.name
 */
public record RequestResponseView(
        Long id,
        Long bloodRequestId,
        Integer status,
        String respondedAt,
        String verifiedAt,
        String createdAt,
        BloodRequestLiteView bloodRequest,
        DonorLiteView donor
) {
    public static RequestResponseView of(
            com.bloodbridge.bloodbridge.entity.RequestResponse rr,
            BloodRequestLiteView bloodRequest,
            DonorLiteView donor) {
        if (rr == null) return null;
        return new RequestResponseView(
                rr.getId(),
                rr.getBloodRequestId(),
                rr.getStatus() == null ? null : rr.getStatus().getValue(),
                rr.getRespondedAt() == null ? null : rr.getRespondedAt().toString(),
                rr.getVerifiedAt() == null ? null : rr.getVerifiedAt().toString(),
                rr.getCreatedAt() == null ? null : rr.getCreatedAt().toString(),
                bloodRequest,
                donor);
    }
}
