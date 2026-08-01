package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestResponseRepository extends JpaRepository<RequestResponse, Long>, JpaSpecificationExecutor<RequestResponse> {

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.bloodRequestId = :requestId AND rr.deletedAt IS NULL ORDER BY rr.respondedAt DESC")
    List<RequestResponse> findByBloodRequestId(@Param("requestId") Long requestId);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.donorId = :donorId AND rr.deletedAt IS NULL ORDER BY rr.respondedAt DESC")
    List<RequestResponse> findByDonorIdOrderByRespondedAtDesc(@Param("donorId") Long donorId);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.donorId = :donorId AND rr.status IN :statuses AND rr.deletedAt IS NULL")
    List<RequestResponse> findByDonorIdAndStatusIn(@Param("donorId") Long donorId, @Param("statuses") List<RequestResponseStatus> statuses);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.donorId = :donorId AND rr.status = :status AND rr.deletedAt IS NULL")
    List<RequestResponse> findByDonorIdAndStatus(@Param("donorId") Long donorId, @Param("status") RequestResponseStatus status);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.bloodRequestId = :requestId AND rr.donorId = :donorId AND rr.deletedAt IS NULL")
    Optional<RequestResponse> findByBloodRequestIdAndDonorId(@Param("requestId") Long requestId, @Param("donorId") Long donorId);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.bloodRequestId = :requestId AND rr.status = :status AND rr.deletedAt IS NULL")
    List<RequestResponse> findByBloodRequestIdAndStatus(@Param("requestId") Long requestId, @Param("status") RequestResponseStatus status);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.bloodRequestId = :requestId AND rr.status IN :statuses AND rr.deletedAt IS NULL")
    List<RequestResponse> findByBloodRequestIdAndStatusIn(@Param("requestId") Long requestId, @Param("statuses") List<RequestResponseStatus> statuses);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.verificationQrCode = :qrCode AND rr.deletedAt IS NULL")
    Optional<RequestResponse> findByVerificationQrCode(@Param("qrCode") String qrCode);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.status = :status AND rr.respondedAt <= :threshold AND rr.deletedAt IS NULL")
    List<RequestResponse> findStalePendingResponses(@Param("status") RequestResponseStatus status, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(rr) FROM RequestResponse rr WHERE rr.donorId = :donorId AND rr.status IN :statuses AND rr.deletedAt IS NULL")
    long countByDonorIdAndStatusIn(@Param("donorId") Long donorId, @Param("statuses") List<RequestResponseStatus> statuses);

    @Query("SELECT COUNT(rr) FROM RequestResponse rr WHERE rr.bloodRequestId = :requestId AND rr.status = :status AND rr.deletedAt IS NULL")
    long countByBloodRequestIdAndStatus(@Param("requestId") Long requestId, @Param("status") RequestResponseStatus status);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.bloodRequestId = :requestId AND rr.donorId IN :donorIds AND rr.deletedAt IS NULL")
    List<RequestResponse> findByBloodRequestIdAndDonorIdIn(@Param("requestId") Long requestId, @Param("donorIds") List<Long> donorIds);

    @Query("SELECT rr FROM RequestResponse rr WHERE rr.bloodRequest.organizationId = :organizationId AND rr.deletedAt IS NULL ORDER BY rr.respondedAt DESC")
    List<RequestResponse> findByBloodRequest_OrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
        SELECT rr.bloodRequestId AS bloodRequestId,
               COUNT(rr) AS total,
               SUM(CASE WHEN rr.status = :accepted THEN 1 ELSE 0 END) AS acceptedCount,
               SUM(CASE WHEN rr.status = :completed THEN 1 ELSE 0 END) AS completedCount
        FROM RequestResponse rr
        WHERE rr.bloodRequestId IN :requestIds AND rr.deletedAt IS NULL
        GROUP BY rr.bloodRequestId
        """)
    List<RequestResponseAggregate> aggregateByBloodRequestIds(
            @Param("requestIds") List<Long> requestIds,
            @Param("accepted") RequestResponseStatus accepted,
            @Param("completed") RequestResponseStatus completed);

    /**
     * GROUP-BY aggregate that powers {@code BloodRequestListView}.
     *   - responsesCount  = COUNT(rr)
     *   - donorsAccepted  = COUNT where status IN (ACCEPTED, COMPLETED) — PENDING excluded
     *                       per user-approved UX semantics 2026-07-30.
     *   - donorsCompleted = COUNT where status = COMPLETED
     *
     * Single query, no N+1 — feeds the list-view projection in one round-trip
     * for any number of request ids.
     */
    @Query("""
        SELECT rr.bloodRequestId AS bloodRequestId,
               COUNT(rr) AS responsesCount,
               SUM(CASE WHEN rr.status IN (:acceptedStatuses) THEN 1 ELSE 0 END) AS donorsAccepted,
               SUM(CASE WHEN rr.status = :completed THEN 1 ELSE 0 END) AS donorsCompleted
        FROM RequestResponse rr
        WHERE rr.bloodRequestId IN :requestIds AND rr.deletedAt IS NULL
        GROUP BY rr.bloodRequestId
        """)
    List<BloodRequestAggregate> aggregateBloodRequestCountsByRequestIds(
            @Param("requestIds") List<Long> requestIds,
            @Param("acceptedStatuses") List<RequestResponseStatus> acceptedStatuses,
            @Param("completed") RequestResponseStatus completed);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rr FROM RequestResponse rr WHERE rr.id = :id AND rr.deletedAt IS NULL")
    Optional<RequestResponse> findByIdWithPessimisticLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE RequestResponse rr SET rr.status = :newStatus WHERE rr.id = :responseId AND rr.status = :expectedStatus AND rr.deletedAt IS NULL")
    int updateStatusWhere(@Param("responseId") Long responseId, @Param("expectedStatus") RequestResponseStatus expectedStatus, @Param("newStatus") RequestResponseStatus newStatus);

    @Modifying
    @Query("UPDATE RequestResponse rr SET rr.status = :newStatus WHERE rr.bloodRequestId = :requestId AND rr.status = :expectedStatus AND rr.deletedAt IS NULL")
    int updateAllByBloodRequestIdWhereStatus(@Param("requestId") Long requestId, @Param("expectedStatus") RequestResponseStatus expectedStatus, @Param("newStatus") RequestResponseStatus newStatus);
}