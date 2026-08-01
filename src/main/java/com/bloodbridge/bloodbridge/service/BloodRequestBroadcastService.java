package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.bloodrequest.domain.BloodRequestBroadcastedEvent;
import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorHealthProfile;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.repository.*;
import com.bloodbridge.bloodbridge.job.DispatchBloodRequestNotifications;
import com.bloodbridge.bloodbridge.service.scoring.DonorScoringService;
import com.bloodbridge.bloodbridge.service.scoring.ScoringSettingsService;
import com.bloodbridge.bloodbridge.shared.audit.AuditLogService;
import com.bloodbridge.bloodbridge.shared.events.DomainEventPublisher;
import com.bloodbridge.bloodbridge.shared.monitoring.BloodBridgeMetrics;
import com.bloodbridge.bloodbridge.util.GeoHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloodRequestBroadcastService {

    private final DonorRepository donorRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final EligibilityLogRepository eligibilityLogRepository;
    private final DonorHealthProfileRepository healthProfileRepository;
    private final DonorScoringService donorScoringService;
    private final ScoringSettingsService scoringSettings;
    private final DispatchBloodRequestNotifications dispatchBloodRequestNotifications;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final BloodBridgeMetrics metrics;

    @Value("${bloodbridge.broadcast.donor-safety-multiplier-normal:2.0}")
    private double donorSafetyMultiplierNormal;

    @Value("${bloodbridge.broadcast.donor-safety-multiplier-critical:2.5}")
    private double donorSafetyMultiplierCritical;

    @Value("${bloodbridge.broadcast.critical-radius-multiplier:3}")
    private int criticalRadiusMultiplier;

    @Value("${bloodbridge.broadcast.radius-expansion-step-km:5}")
    private int radiusExpansionStepKm;

    @Value("${bloodbridge.broadcast.max-search-radius-km:25}")
    private int maxSearchRadiusKm;

    @Value("${bloodbridge.broadcast.notification-cooldown-critical-hours:0.5}")
    private double notificationCooldownCriticalHours;

    @Value("${bloodbridge.broadcast.notification-cooldown-normal-hours:2.0}")
    private double notificationCooldownNormalHours;

    @Transactional
    public int broadcast(BloodRequest bloodRequest) {
        if (!hasValidLocation(bloodRequest)) {
            log.warn("Blood request {} missing required location data", bloodRequest.getId());
            return 0;
        }

        return metrics.timeBroadcast(() -> {
            try {
                List<Donor> eligibleDonors = findEligibleDonorsWithExpansion(bloodRequest);
                updateBroadcastStatus(bloodRequest);

                int totalEligible = eligibleDonors.size();
                String urgency = bloodRequest.getUrgencyLevel().isCritical() ? "critical" : "normal";

                try {
                    DonorScoringService.ScoreSelectionResult scoringResult =
                            donorScoringService.scoreAndSelect(eligibleDonors, urgency);

                    List<Donor> selectedDonors = scoringResult.getSelected();

                    log.info("Donors filtered by scoring for request {}: total={}, after_scoring={}",
                            bloodRequest.getId(), totalEligible, selectedDonors.size());

                    int responsesCreated = createPendingResponses(bloodRequest, selectedDonors);
                    log.info("Created {} pending responses for request {}", responsesCreated, bloodRequest.getId());

                    int notificationsQueued = notifyEligibleDonors(bloodRequest, selectedDonors);

                    // Publish domain event
                    List<Long> donorIds = selectedDonors.stream().map(Donor::getId).collect(Collectors.toList());
                    eventPublisher.publish(new BloodRequestBroadcastedEvent(
                            bloodRequest.getId(), bloodRequest.getOrganizationId(),
                            selectedDonors.size(), donorIds,
                            bloodRequest.getActualSearchRadiusKm() != null ? bloodRequest.getActualSearchRadiusKm() : bloodRequest.getSearchRadiusKm()));

                    auditLogService.logSimple("BloodRequest", bloodRequest.getId(), "BROADCASTED", bloodRequest.getOrganizationId());
                    metrics.incrementBroadcast();

                    log.info("Blood request {} broadcasted successfully: donors_found={}, donors_notified={}, notifications={}",
                            bloodRequest.getId(), totalEligible, selectedDonors.size(), notificationsQueued);

                    return selectedDonors.size();
                } catch (Exception e) {
                    bloodRequest.setStatus(BloodRequestStatus.PENDING);
                    bloodRequest.setBroadcastedAt(null);
                    bloodRequest.setActualSearchRadiusKm(null);
                    bloodRequestRepository.save(bloodRequest);
                    throw e;
                }
            } catch (Exception e) {
                log.error("Failed to broadcast blood request {}: {}", bloodRequest.getId(), e.getMessage());
                throw e;
            }
        });
    }

    private boolean hasValidLocation(BloodRequest bloodRequest) {
        boolean hasCoordinates = bloodRequest.getLat() != null
                && bloodRequest.getLng() != null
                && bloodRequest.getSearchRadiusKm() > 0;

        boolean hasGovernorate = bloodRequest.getOrganization() != null
                && bloodRequest.getOrganization().getGovernorateId() != null;

        return hasCoordinates || hasGovernorate;
    }

    public List<Donor> findEligibleDonorsWithExpansion(BloodRequest bloodRequest) {
        List<Integer> compatibleBloodTypeValues = Arrays.stream(bloodRequest.getBloodType().getCompatibleDonorTypes())
                .map(BloodType::getValue)
                .collect(Collectors.toList());

        if (compatibleBloodTypeValues.isEmpty()) {
            return new ArrayList<>();
        }

        boolean isCritical = bloodRequest.getUrgencyLevel().isCritical();
        int targetDonorCount = calculateTargetDonorCount(bloodRequest, isCritical);
        int currentRadius = getInitialSearchRadius(bloodRequest, isCritical);

        Set<Long> matchedDonorIds = new HashSet<>();
        List<Donor> matchedDonors = new ArrayList<>();
        int expansionAttempts = 0;

        while (matchedDonors.size() < targetDonorCount && currentRadius <= maxSearchRadiusKm) {
            List<Donor> newDonors = searchDonorsInRadius(
                    bloodRequest, compatibleBloodTypeValues, currentRadius, isCritical, matchedDonorIds);

            for (Donor donor : newDonors) {
                if (!matchedDonorIds.contains(donor.getId())) {
                    matchedDonorIds.add(donor.getId());
                    matchedDonors.add(donor);
                }
            }

            log.info("Radius expansion attempt for request {}: radius={}km, donors_found={}, target={}, attempt={}",
                    bloodRequest.getId(), currentRadius, matchedDonors.size(), targetDonorCount, expansionAttempts);

            if (matchedDonors.size() >= targetDonorCount) {
                break;
            }

            if (currentRadius >= maxSearchRadiusKm) {
                break;
            }

            currentRadius += radiusExpansionStepKm;
            expansionAttempts++;
        }

        List<Donor> finalDonors = new ArrayList<>(matchedDonors);
        if (matchedDonors.size() < targetDonorCount && !isCritical) {
            List<Donor> unknownDonors = searchUnknownDonors(bloodRequest, currentRadius, isCritical, matchedDonorIds);
            for (Donor donor : unknownDonors) {
                if (!matchedDonorIds.contains(donor.getId())) {
                    matchedDonorIds.add(donor.getId());
                    finalDonors.add(donor);
                }
            }
            log.info("Added UNKNOWN blood type donors as fallback for request {}: matched={}, unknown={}, total={}",
                    bloodRequest.getId(), matchedDonors.size(), unknownDonors.size(), finalDonors.size());
        }

        saveExpansionResults(bloodRequest, currentRadius);

        log.info("Progressive expansion completed for request {}: initialRadius={}km, finalRadius={}km, attempts={}, donors={}, target={}",
                bloodRequest.getId(), bloodRequest.getSearchRadiusKm(), currentRadius,
                expansionAttempts, finalDonors.size(), targetDonorCount);

        return finalDonors;
    }

    private int calculateTargetDonorCount(BloodRequest bloodRequest, boolean isCritical) {
        double multiplier = isCritical ? donorSafetyMultiplierCritical : donorSafetyMultiplierNormal;
        return (int) Math.ceil(bloodRequest.getUnitsNeeded() * multiplier);
    }

    private int getInitialSearchRadius(BloodRequest bloodRequest, boolean isCritical) {
        return isCritical
                ? bloodRequest.getSearchRadiusKm() * criticalRadiusMultiplier
                : bloodRequest.getSearchRadiusKm();
    }

    private List<Donor> searchDonorsInRadius(
            BloodRequest bloodRequest, List<Integer> compatibleBloodTypeValues,
            int radiusKm, boolean isCritical, Set<Long> excludedDonorIds) {

        double cooldownHours = isCritical ? notificationCooldownCriticalHours : notificationCooldownNormalHours;
        LocalDateTime cooldownThreshold = LocalDateTime.now().minusHours((long) cooldownHours);

        List<Donor> donors = findDonorsByLocationOrGovernorate(bloodRequest, radiusKm);

        donors = donors.stream()
                .filter(d -> isCompatible(d, compatibleBloodTypeValues))
                .filter(this::isEligible)
                .filter(d -> !hasPermanentExclusion(d.getId()))
                .filter(d -> !hasExistingResponse(d.getId(), bloodRequest.getId()))
                .filter(d -> !hasRecentNotification(d.getId(), bloodRequest.getId(), cooldownThreshold))
                .filter(d -> !excludedDonorIds.contains(d.getId()))
                .collect(Collectors.toList());
        return donors;
    }

    private List<Donor> searchUnknownDonors(
            BloodRequest bloodRequest, int radiusKm, boolean isCritical, Set<Long> excludedDonorIds) {

        if (isCritical) {
            return new ArrayList<>();
        }

        List<Donor> donors = findDonorsByLocationOrGovernorate(bloodRequest, radiusKm);

        double cooldownHours = isCritical ? notificationCooldownCriticalHours : notificationCooldownNormalHours;
        LocalDateTime cooldownThreshold = LocalDateTime.now().minusHours((long) cooldownHours);

        donors = donors.stream()
                .filter(d -> hasUnknownBloodType(d.getId()))
                .filter(this::isEligible)
                .filter(d -> !hasPermanentExclusion(d.getId()))
                .filter(d -> !hasExistingResponse(d.getId(), bloodRequest.getId()))
                .filter(d -> !hasRecentNotification(d.getId(), bloodRequest.getId(), cooldownThreshold))
                .filter(d -> !excludedDonorIds.contains(d.getId()))
                .collect(Collectors.toList());

        return donors;
    }

    private List<Donor> findDonorsByLocationOrGovernorate(BloodRequest bloodRequest, int radiusKm) {
        Double lat = bloodRequest.getLat();
        Double lng = bloodRequest.getLng();
        Long governorateId = bloodRequest.getOrganization() != null
                ? bloodRequest.getOrganization().getGovernorateId() : null;

        if (lat != null && lng != null && lat != 0 && lng != 0) {
            GeoHelper.BoundingBox bbox = GeoHelper.calculateBoundingBox(lat, lng, radiusKm);
            return new ArrayList<>(donorRepository.findWithinRadiusWithDistance(
                    lat, lng, radiusKm,
                    bbox.minLat(), bbox.maxLat(),
                    bbox.minLng(), bbox.maxLng()));
        }
        if (governorateId != null) {
            return donorRepository.findByGovernorateFallback(governorateId);
        }
        return new ArrayList<>();
    }

    private boolean isCompatible(Donor donor, List<Integer> compatibleBloodTypeValues) {
        DonorHealthProfile profile = donor.getHealthProfile();
        if (profile == null) return false;

        if (profile.getVerifiedBloodType() != null) {
            return compatibleBloodTypeValues.contains(profile.getVerifiedBloodType().getValue());
        }
        if (profile.getBloodType() != null) {
            return compatibleBloodTypeValues.contains(profile.getBloodType().getValue());
        }
        return false;
    }

    private boolean isEligible(Donor donor) {
        DonorHealthProfile profile = donor.getHealthProfile();
        if (profile == null || !Boolean.TRUE.equals(profile.getIsEligible())) return false;

        if (profile.getNextEligibleDate() != null
                && profile.getNextEligibleDate().isAfter(java.time.LocalDate.now())) {
            return false;
        }
        return true;
    }

    private boolean hasPermanentExclusion(Long donorId) {
        return eligibilityLogRepository.findPermanentIneligibilityByDonorId(donorId).size() > 0;
    }

    private boolean hasExistingResponse(Long donorId, Long bloodRequestId) {
        return requestResponseRepository
                .findByBloodRequestIdAndDonorId(bloodRequestId, donorId)
                .isPresent();
    }

    private boolean hasRecentNotification(Long donorId, Long bloodRequestId, LocalDateTime cooldownThreshold) {
        List<RequestResponse> recentResponses = requestResponseRepository
                .findByDonorIdAndStatusIn(donorId, List.of(
                        RequestResponseStatus.PENDING, RequestResponseStatus.ACCEPTED,
                        RequestResponseStatus.IGNORED, RequestResponseStatus.DECLINED,
                        RequestResponseStatus.NO_SHOW));

        return recentResponses.stream()
                .anyMatch(r -> !r.getBloodRequestId().equals(bloodRequestId)
                        && r.getRespondedAt() != null
                        && r.getRespondedAt().isAfter(cooldownThreshold));
    }

    private boolean hasUnknownBloodType(Long donorId) {
        DonorHealthProfile profile = healthProfileRepository.findByDonorId(donorId).orElse(null);
        return profile != null
                && profile.getBloodType() == BloodType.UNKNOWN
                && profile.getVerifiedBloodType() == null;
    }

    private int createPendingResponses(BloodRequest bloodRequest, List<Donor> donors) {
        int count = 0;
        for (Donor donor : donors) {
            if (requestResponseRepository
                    .findByBloodRequestIdAndDonorId(bloodRequest.getId(), donor.getId())
                    .isPresent()) {
                continue;
            }

            RequestResponse response = new RequestResponse();
            response.setBloodRequestId(bloodRequest.getId());
            response.setDonorId(donor.getId());
            response.setStatus(RequestResponseStatus.PENDING);
            if (bloodRequest.getLat() != null && bloodRequest.getLng() != null
                    && donor.getLat() != null && donor.getLng() != null) {
                response.setDistance((float) GeoHelper.calculateDistance(
                        bloodRequest.getLat(), bloodRequest.getLng(),
                        donor.getLat(), donor.getLng()));
            }
            requestResponseRepository.saveAndFlush(response);
            count++;
        }
        return count;
    }

    private int notifyEligibleDonors(BloodRequest bloodRequest, List<Donor> donors) {
        Map<Long, Double> donorData = new HashMap<>();
        for (Donor donor : donors) {
            if (donor.getUser() != null) {
                Double distance = null;
                if (bloodRequest.getLat() != null && bloodRequest.getLng() != null
                        && donor.getLat() != null && donor.getLng() != null) {
                    distance = GeoHelper.calculateDistance(
                            bloodRequest.getLat(), bloodRequest.getLng(),
                            donor.getLat(), donor.getLng());
                }
                donorData.put(donor.getUser().getId(), distance);
            }
        }

        if (!donorData.isEmpty()) {
            dispatchBloodRequestNotifications.dispatchBatches(bloodRequest.getId(), donorData);
        }

        return donorData.size();
    }

    private void saveExpansionResults(BloodRequest bloodRequest, int finalRadius) {
        bloodRequest.setActualSearchRadiusKm(finalRadius);
        bloodRequestRepository.save(bloodRequest);
    }

    private void updateBroadcastStatus(BloodRequest bloodRequest) {
        bloodRequest.setStatus(BloodRequestStatus.BROADCASTED);
        bloodRequest.setBroadcastedAt(LocalDateTime.now());
        bloodRequestRepository.save(bloodRequest);
    }
}