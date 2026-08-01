package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.DonorHealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorHealthProfileRepository extends JpaRepository<DonorHealthProfile, Long> {

    Optional<DonorHealthProfile> findByDonorId(Long donorId);

    @Query("SELECT dhp FROM DonorHealthProfile dhp WHERE dhp.isEligible = true AND (dhp.nextEligibleDate IS NULL OR dhp.nextEligibleDate <= :today) AND dhp.donor.deletedAt IS NULL")
    List<DonorHealthProfile> findCurrentlyEligible(@Param("today") LocalDate today);

    @Query("SELECT dhp FROM DonorHealthProfile dhp WHERE dhp.chronicDisease = true AND dhp.donor.deletedAt IS NULL")
    List<DonorHealthProfile> findChronicDiseaseDonors();

    @Query("SELECT dhp FROM DonorHealthProfile dhp WHERE dhp.verifiedBloodType = :bloodType AND dhp.donor.deletedAt IS NULL")
    List<DonorHealthProfile> findByVerifiedBloodType(@Param("bloodType") com.bloodbridge.bloodbridge.enumtype.BloodType bloodType);
}