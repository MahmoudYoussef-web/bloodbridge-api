package com.bloodbridge.bloodbridge.dto;

import java.time.LocalDateTime;

/**
 * Wire mirror of {@link com.bloodbridge.bloodbridge.entity.BloodRequest}
 * restricted to the exact fields the four org-side screens read, plus
 * three aggregate counts the UI has been trying to display since before
 * this sprint (and was always rendering {@code 0} for).
 *
 * Replaces the raw {@code BloodRequest} entity in:
 *   - GET /org/blood-requests          (OrgBloodRequests, OrgDashboard, OrgStatistics)
 *   - GET /org/blood-requests/{id}     (OrgViewBloodRequest)
 *
 * Aggregate semantics (locked by user decision 2026-07-30):
 *   - responsesCount   = total non-deleted RequestResponse rows for this request
 *   - donorsAccepted   = count where status IN (ACCEPTED, COMPLETED) — PENDING excluded.
 *                        Uses the UX meaning of "Accepted" (responded yes), not the
 *                        technical isActiveResponse() definition which also includes PENDING.
 *   - donorsCompleted  = count where status = COMPLETED
 */
public record BloodRequestListView(
        Long id,
        Integer bloodType,
        Integer unitsNeeded,
        Integer urgencyLevel,
        Integer status,
        Integer searchRadiusKm,
        String additionalNotes,
        LocalDateTime createdAt,
        Long responsesCount,
        Long donorsAccepted,
        Long donorsCompleted
) {
    public static BloodRequestListView of(
            com.bloodbridge.bloodbridge.entity.BloodRequest br,
            Long responsesCount,
            Long donorsAccepted,
            Long donorsCompleted) {
        if (br == null) return null;
        return new BloodRequestListView(
                br.getId(),
                br.getBloodType() == null ? null : br.getBloodType().getValue(),
                br.getUnitsNeeded(),
                br.getUrgencyLevel() == null ? null : br.getUrgencyLevel().getValue(),
                br.getStatus() == null ? null : br.getStatus().getValue(),
                br.getSearchRadiusKm(),
                br.getAdditionalNotes(),
                br.getCreatedAt(),
                responsesCount,
                donorsAccepted,
                donorsCompleted);
    }
}
