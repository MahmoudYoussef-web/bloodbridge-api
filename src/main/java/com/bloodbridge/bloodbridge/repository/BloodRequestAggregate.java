package com.bloodbridge.bloodbridge.repository;

/**
 * Hibernate projection for the GROUP-BY aggregate that powers the
 * {@code BloodRequestListView} counters.  Mirrors the
 * {@link RequestResponseAggregate} pattern from Item 18.
 *
 * Status filters are applied in the JPQL via explicit enum parameters
 * so callers can lock in the user-approved semantics:
 *   - {@code responsesCount}  — count of all non-deleted responses
 *   - {@code donorsAccepted}  — count where status ∈ {ACCEPTED, COMPLETED} (PENDING excluded)
 *   - {@code donorsCompleted} — count where status = COMPLETED
 */
public interface BloodRequestAggregate {
    Long getBloodRequestId();
    Long getResponsesCount();
    Long getDonorsAccepted();
    Long getDonorsCompleted();
}
