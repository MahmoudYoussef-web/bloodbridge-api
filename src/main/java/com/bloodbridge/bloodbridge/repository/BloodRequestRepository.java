package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long>, JpaSpecificationExecutor<BloodRequest> {

    List<BloodRequest> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    @Query("SELECT br FROM BloodRequest br WHERE br.status IN :statuses AND br.fulfilledAt IS NULL AND br.deletedAt IS NULL")
    List<BloodRequest> findByStatusIn(@Param("statuses") List<BloodRequestStatus> statuses);

    @Query("SELECT br FROM BloodRequest br WHERE br.status = :status AND br.fulfilledAt IS NULL AND br.deletedAt IS NULL ORDER BY br.createdAt DESC")
    List<BloodRequest> findActiveByStatus(@Param("status") BloodRequestStatus status);

    @Query("SELECT br FROM BloodRequest br WHERE br.status IN :statuses AND br.createdAt <= :threshold AND br.fulfilledAt IS NULL AND br.deletedAt IS NULL")
    List<BloodRequest> findByStatusInAndCreatedAtBefore(
            @Param("statuses") List<BloodRequestStatus> statuses,
            @Param("threshold") LocalDateTime threshold);

    @Query("SELECT br FROM BloodRequest br WHERE br.organizationId = :orgId AND br.status IN :statuses AND br.deletedAt IS NULL")
    List<BloodRequest> findByOrganizationIdAndStatusIn(@Param("orgId") Long orgId, @Param("statuses") List<BloodRequestStatus> statuses);

    @Query("SELECT br FROM BloodRequest br WHERE br.id = :id AND br.deletedAt IS NULL")
    Optional<BloodRequest> findByIdNotDeleted(@Param("id") Long id);

    @Query("SELECT br FROM BloodRequest br WHERE br.id = :id AND br.status = :status AND br.fulfilledAt IS NULL AND br.deletedAt IS NULL")
    Optional<BloodRequest> findActiveById(@Param("id") Long id, @Param("status") BloodRequestStatus status);

    @Query("SELECT br FROM BloodRequest br WHERE br.lat IS NOT NULL AND br.lng IS NOT NULL AND br.status IN :statuses AND br.deletedAt IS NULL ORDER BY br.createdAt DESC")
    List<BloodRequest> findActiveNearLocation(@Param("statuses") List<BloodRequestStatus> statuses);

    @Query("""
        SELECT br FROM BloodRequest br
        WHERE br.status IN :statuses
          AND br.deletedAt IS NULL
          AND br.fulfilledAt IS NULL
          AND br.id NOT IN (
              SELECT rr.bloodRequestId FROM RequestResponse rr
              WHERE rr.donorId = :donorId AND rr.deletedAt IS NULL
          )
        ORDER BY br.broadcastedAt DESC
        """)
    List<BloodRequest> findActiveForDonor(@Param("donorId") Long donorId, @Param("statuses") List<BloodRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT br FROM BloodRequest br WHERE br.id = :id AND br.deletedAt IS NULL")
    Optional<BloodRequest> findByIdWithPessimisticLock(@Param("id") Long id);
}