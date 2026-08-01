package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.EligibilityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityLogRepository extends JpaRepository<EligibilityLog, Long> {

    List<EligibilityLog> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    @Query("SELECT el FROM EligibilityLog el WHERE el.donorId = :donorId AND el.isPermanent = true AND el.isEligible = false")
    List<EligibilityLog> findPermanentIneligibilityByDonorId(@Param("donorId") Long donorId);

    @Query("SELECT el FROM EligibilityLog el WHERE el.donorId = :donorId ORDER BY el.createdAt DESC")
    List<EligibilityLog> findByDonorIdNotDeleted(@Param("donorId") Long donorId);
}