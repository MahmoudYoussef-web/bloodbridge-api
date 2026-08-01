package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.DonorPredictiveScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorPredictiveScoreRepository extends JpaRepository<DonorPredictiveScore, Long> {

    Optional<DonorPredictiveScore> findByDonorId(Long donorId);

    List<DonorPredictiveScore> findByDonorIdIn(List<Long> donorIds);

    @Query("SELECT dps FROM DonorPredictiveScore dps WHERE dps.donorId IN :donorIds AND (dps.computedAt IS NULL OR dps.computedAt >= :threshold)")
    List<DonorPredictiveScore> findFreshScoresByDonorIds(
            @Param("donorIds") List<Long> donorIds,
            @Param("threshold") LocalDateTime threshold);

    void deleteByDonorIdIn(List<Long> donorIds);
}