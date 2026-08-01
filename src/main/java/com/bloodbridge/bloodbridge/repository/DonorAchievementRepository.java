package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.DonorAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorAchievementRepository extends JpaRepository<DonorAchievement, Long> {

    @Query("SELECT da FROM DonorAchievement da WHERE da.donorId = :donorId AND da.deletedAt IS NULL ORDER BY da.earnedAt DESC")
    List<DonorAchievement> findByDonorId(@Param("donorId") Long donorId);

    @Query("SELECT da FROM DonorAchievement da WHERE da.donorId = :donorId AND da.achievementId = :achievementId AND da.deletedAt IS NULL")
    Optional<DonorAchievement> findByAchievementIdAndDonorId(@Param("achievementId") Long achievementId, @Param("donorId") Long donorId);

    @Query("SELECT CASE WHEN COUNT(da) > 0 THEN true ELSE false END FROM DonorAchievement da WHERE da.donorId = :donorId AND da.achievementId = :achievementId AND da.deletedAt IS NULL AND da.earnedAt IS NOT NULL")
    boolean existsByDonorIdAndAchievementIdAndEarned(@Param("donorId") Long donorId, @Param("achievementId") Long achievementId);

    @Query("SELECT COUNT(da) FROM DonorAchievement da WHERE da.donorId = :donorId AND da.deletedAt IS NULL AND da.earnedAt IS NOT NULL")
    long countByDonorIdAndEarnedTrue(@Param("donorId") Long donorId);
}