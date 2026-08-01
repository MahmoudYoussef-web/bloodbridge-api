package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

    Optional<Donor> findByUserId(Long userId);

    Optional<Donor> findByNationalId(String nationalId);

    List<Donor> findByGovernorateId(Long governorateId);

    /**
     * Find donors within a radius using Haversine formula
     * Includes bounding box optimization for better performance
     */
    @Query(value = """
        SELECT d.*
        FROM donors d
        WHERE d.deleted_at IS NULL
          AND d.lat IS NOT NULL
          AND d.lng IS NOT NULL
          AND d.lat BETWEEN :minLat AND :maxLat
          AND d.lng BETWEEN :minLng AND :maxLng
        AND (6371 * acos(
              cos(radians(:lat)) * cos(radians(d.lat)) * cos(radians(d.lng) - radians(:lng)) +
              sin(radians(:lat)) * sin(radians(d.lat))
          )) <= :radiusKm
        ORDER BY (6371 * acos(
              cos(radians(:lat)) * cos(radians(d.lat)) * cos(radians(d.lng) - radians(:lng)) +
              sin(radians(:lat)) * sin(radians(d.lat))
          )) ASC
        """, nativeQuery = true)
    List<Donor> findWithinRadiusWithDistance(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Integer radiusKm,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    /**
     * Find donors by governorate fallback (when no GPS coordinates)
     */
    @Query("SELECT d FROM Donor d WHERE d.governorateId = :governorateId AND d.lat IS NULL AND d.lng IS NULL AND d.deletedAt IS NULL")
    List<Donor> findByGovernorateFallback(@Param("governorateId") Long governorateId);

    /**
     * Find eligible donors within radius compatible with blood types
     */
    @Query("""
        SELECT d FROM Donor d
        JOIN d.healthProfile dhp
        WHERE d.deletedAt IS NULL
          AND dhp.isEligible = true
          AND (dhp.nextEligibleDate IS NULL OR dhp.nextEligibleDate <= CURRENT_DATE)
          AND (dhp.verifiedBloodType IN :bloodTypes OR (dhp.verifiedBloodType IS NULL AND dhp.bloodType IN :bloodTypes))
        """)
    List<Donor> findEligibleByBloodTypes(@Param("bloodTypes") List<BloodType> bloodTypes);

    /**
     * Find donors not recently notified (cooldown check)
     */
    @Query("""
        SELECT d FROM Donor d
        WHERE d.id NOT IN (
            SELECT rr.donorId FROM RequestResponse rr
            WHERE rr.bloodRequestId != :requestId
              AND rr.status IN :recentStatuses
              AND rr.respondedAt >= :cooldownThreshold
              AND rr.deletedAt IS NULL
        )
    """)
    List<Donor> findDonorsNotInCooldown(
            @Param("requestId") Long requestId,
            @Param("recentStatuses") List<RequestResponseStatus> recentStatuses,
            @Param("cooldownThreshold") LocalDateTime cooldownThreshold);

    /**
     * Find donors without permanent ineligibility
     */
    @Query("""
        SELECT d FROM Donor d
        WHERE d.id NOT IN (
            SELECT el.donorId FROM EligibilityLog el
            WHERE el.isEligible = false AND el.isPermanent = true
        )
    """)
    List<Donor> findWithoutPermanentIneligibility();

    /**
     * Find UNKNOWN blood type donors as fallback
     */
    @Query("""
        SELECT d FROM Donor d
        JOIN d.healthProfile dhp
        WHERE d.deletedAt IS NULL
          AND dhp.isEligible = true
          AND (dhp.nextEligibleDate IS NULL OR dhp.nextEligibleDate <= CURRENT_DATE)
          AND dhp.bloodType = BloodType.UNKNOWN
          AND dhp.verifiedBloodType IS NULL
    """)
    List<Donor> findUnknownBloodTypeEligible();
}