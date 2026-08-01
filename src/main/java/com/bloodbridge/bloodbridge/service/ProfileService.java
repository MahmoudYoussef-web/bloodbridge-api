package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.*;
import com.bloodbridge.bloodbridge.entity.*;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.exception.ResourceNotFoundException;
import com.bloodbridge.bloodbridge.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final DonorRepository donorRepository;
    private final DonorHealthProfileRepository healthProfileRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final AchievementRepository achievementRepository;
    private final DonorAchievementRepository donorAchievementRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DonorProfileResponse getDonorProfile(Long userId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));
        User user = donor.getUser();
        DonorHealthProfile hp = donor.getHealthProfile();

        return DonorProfileResponse.builder()
                .id(donor.getId())
                .userId(donor.getUserId())
                .name(user != null ? user.getName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .nationalId(donor.getNationalId())
                .gender(donor.getGender())
                .birthDate(donor.getBirthDate())
                .governorateId(donor.getGovernorateId())
                .autoLocationAddress(donor.getAutoLocationAddress())
                .lat(donor.getLat())
                .lng(donor.getLng())
                .points(donor.getPoints())
                .level(donor.getLevel())
                .weight(hp != null ? hp.getWeight() : null)
                .height(hp != null ? hp.getHeight() : null)
                .bloodType(hp != null ? hp.getBloodType() : null)
                .verifiedBloodType(hp != null ? hp.getVerifiedBloodType() : null)
                .chronicDisease(hp != null ? hp.getChronicDisease() : null)
                .recentDonation(hp != null ? hp.getRecentDonation() : null)
                .infection(hp != null ? hp.getInfection() : null)
                .isEligible(hp != null ? hp.getIsEligible() : null)
                .hasRecentSurgery(hp != null ? hp.getHasRecentSurgery() : null)
                .surgeryDate(hp != null ? hp.getSurgeryDate() : null)
                .nextEligibleDate(hp != null ? hp.getNextEligibleDate() : null)
                .lastDonationDate(hp != null ? hp.getLastDonationDate() : null)
                .totalDonations(hp != null ? hp.getTotalDonations() : null)
                .createdAt(donor.getCreatedAt())
                .build();
    }

    @Transactional
    public DonorProfileResponse updateDonorProfile(Long userId, DonorProfileUpdateRequest request) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));
        User user = donor.getUser();

        if (user != null) {
            if (request.getName() != null) user.setName(request.getName());
            if (request.getPhone() != null) user.setPhone(request.getPhone());
            userRepository.save(user);
        }

        DonorHealthProfile hp = donor.getHealthProfile();
        if (hp == null) {
            hp = new DonorHealthProfile();
            hp.setDonor(donor);
            hp.setIsEligible(true);
        }

        if (request.getWeight() != null) hp.setWeight(request.getWeight());
        if (request.getHeight() != null) hp.setHeight(request.getHeight());
        if (request.getBloodType() != null) hp.setBloodType(request.getBloodType());
        if (request.getChronicDisease() != null) hp.setChronicDisease(request.getChronicDisease());
        if (request.getInfection() != null) hp.setInfection(request.getInfection());
        if (request.getHasRecentSurgery() != null) hp.setHasRecentSurgery(request.getHasRecentSurgery());
        if (request.getSurgeryDate() != null) hp.setSurgeryDate(request.getSurgeryDate());

        healthProfileRepository.save(hp);

        return getDonorProfile(userId);
    }

    @Transactional(readOnly = true)
    public OrganizationProfileResponse getOrgProfile(Long userId) {
        Organization org = organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        List<Integer> workingDaysList = null;
        if (org.getWorkingDays() != null) {
            try {
                workingDaysList = objectMapper.readValue(org.getWorkingDays(), new TypeReference<List<Integer>>() {});
            } catch (JsonProcessingException e) {
                workingDaysList = List.of();
            }
        }

        return OrganizationProfileResponse.builder()
                .id(org.getId())
                .userId(org.getUserId())
                .orgName(org.getOrgName())
                .slug(org.getSlug())
                .description(org.getDescription())
                .licenseNumber(org.getLicenseNumber())
                .licenseDocumentPath(org.getLicenseDocumentPath())
                .responsiblePersonName(org.getResponsiblePersonName())
                .responsiblePersonPosition(org.getResponsiblePersonPosition())
                .responsiblePersonEmail(org.getResponsiblePersonEmail())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .streetAddress(org.getStreetAddress())
                .autoLocationAddress(org.getAutoLocationAddress())
                .lat(org.getLat())
                .lng(org.getLng())
                .openingTime(org.getOpeningTime() != null ? org.getOpeningTime().toString() : null)
                .closingTime(org.getClosingTime() != null ? org.getClosingTime().toString() : null)
                .workingDays(workingDaysList)
                .dailyCapacity(org.getDailyCapacity())
                .governorateId(org.getGovernorateId())
                .approvalStatus(org.getApprovalStatus() != null ? org.getApprovalStatus().ordinal() : null)
                .rejectionReason(org.getRejectionReason())
                .createdAt(org.getCreatedAt() != null ? org.getCreatedAt().toString() : null)
                .updatedAt(org.getUpdatedAt() != null ? org.getUpdatedAt().toString() : null)
                .build();
    }

    @Transactional
    public OrganizationProfileResponse updateOrgProfile(Long userId, OrganizationProfileUpdateRequest request) {
        Organization org = organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (request.getOrgName() != null) org.setOrgName(request.getOrgName());
        if (request.getContactEmail() != null) org.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) org.setContactPhone(request.getContactPhone());
        if (request.getDescription() != null) org.setDescription(request.getDescription());
        if (request.getStreetAddress() != null) org.setStreetAddress(request.getStreetAddress());
        if (request.getAutoLocationAddress() != null) org.setAutoLocationAddress(request.getAutoLocationAddress());
        if (request.getLat() != null) org.setLat(request.getLat());
        if (request.getLng() != null) org.setLng(request.getLng());
        if (request.getOpeningTime() != null) org.setOpeningTime(LocalTime.parse(request.getOpeningTime()));
        if (request.getClosingTime() != null) org.setClosingTime(LocalTime.parse(request.getClosingTime()));
        if (request.getWorkingDays() != null) {
            try {
                org.setWorkingDays(objectMapper.writeValueAsString(request.getWorkingDays()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize working days", e);
            }
        }
        if (request.getDailyCapacity() != null) org.setDailyCapacity(request.getDailyCapacity());
        if (request.getLicenseNumber() != null) org.setLicenseNumber(request.getLicenseNumber());

        organizationRepository.save(org);
        return getOrgProfile(userId);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestCardResponse> getDonorActiveRequests(Long userId, Double donorLat, Double donorLng) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));

        List<BloodRequest> requests = bloodRequestRepository.findActiveForDonor(
                donor.getId(), List.of(BloodRequestStatus.BROADCASTED));

        List<Long> requestIds = requests.stream().map(BloodRequest::getId).collect(Collectors.toList());
        Map<Long, RequestResponseAggregate> aggregateByRequestId = requestIds.isEmpty()
                ? Collections.emptyMap()
                : requestResponseRepository.aggregateByBloodRequestIds(
                        requestIds, RequestResponseStatus.ACCEPTED, RequestResponseStatus.COMPLETED)
                    .stream()
                    .collect(Collectors.toMap(RequestResponseAggregate::getBloodRequestId, a -> a));

        Map<Long, RequestResponseStatus> myStatusByRequestId = requestIds.stream()
                .collect(Collectors.toMap(id -> id, id -> null));

        requestResponseRepository.findByDonorIdOrderByRespondedAtDesc(donor.getId()).stream()
                .filter(r -> requestIds.contains(r.getBloodRequestId()))
                .forEach(r -> myStatusByRequestId.put(r.getBloodRequestId(), r.getStatus()));

        return requests.stream()
                .map(req -> buildCardResponse(req, donor, myStatusByRequestId, aggregateByRequestId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BloodRequestCardResponse getDonorRequestDetail(Long userId, Long requestId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));

        BloodRequest request = bloodRequestRepository.findByIdNotDeleted(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        Map<Long, RequestResponseStatus> myStatusByRequestId = new HashMap<>();
        requestResponseRepository.findByDonorIdOrderByRespondedAtDesc(donor.getId()).stream()
                .filter(r -> r.getBloodRequestId().equals(requestId))
                .findFirst()
                .ifPresent(r -> myStatusByRequestId.put(requestId, r.getStatus()));

        Map<Long, RequestResponseAggregate> aggregate = new HashMap<>();
        List<RequestResponseAggregate> aggList = requestResponseRepository.aggregateByBloodRequestIds(
                List.of(requestId), RequestResponseStatus.ACCEPTED, RequestResponseStatus.COMPLETED);
        if (!aggList.isEmpty()) {
            aggregate.put(requestId, aggList.get(0));
        }

        return buildCardResponse(request, donor, myStatusByRequestId, aggregate);
    }

    private BloodRequestCardResponse buildCardResponse(BloodRequest req, Donor donor,
                                                       Map<Long, RequestResponseStatus> myStatusByRequestId,
                                                       Map<Long, RequestResponseAggregate> aggregateByRequestId) {
        double distance = 0;
        if (req.getLat() != null && req.getLng() != null && donor.getLat() != null && donor.getLng() != null) {
            distance = haversine(donor.getLat(), donor.getLng(), req.getLat(), req.getLng());
        }

        RequestResponseStatus myStatus = myStatusByRequestId.get(req.getId());
        Integer responsesCount = null;
        Integer donorsAccepted = null;
        Integer donorsCompleted = null;

        RequestResponseAggregate agg = aggregateByRequestId.get(req.getId());
        if (agg != null) {
            responsesCount = agg.getTotal() != null ? agg.getTotal().intValue() : 0;
            donorsAccepted = agg.getAcceptedCount() != null ? agg.getAcceptedCount().intValue() : 0;
            donorsCompleted = agg.getCompletedCount() != null ? agg.getCompletedCount().intValue() : 0;
        }

        return BloodRequestCardResponse.builder()
                .id(req.getId())
                .organizationId(req.getOrganizationId())
                .organizationName(req.getOrganization() != null ? req.getOrganization().getOrgName() : null)
                .bloodType(req.getBloodType())
                .unitsNeeded(req.getUnitsNeeded())
                .urgencyLevel(req.getUrgencyLevel())
                .additionalNotes(req.getAdditionalNotes())
                .searchRadiusKm(req.getSearchRadiusKm())
                .lat(req.getLat())
                .lng(req.getLng())
                .locationAddress(req.getLocationAddress())
                .status(req.getStatus())
                .broadcastedAt(req.getBroadcastedAt() != null ? req.getBroadcastedAt().toString() : null)
                .fulfilledAt(req.getFulfilledAt() != null ? req.getFulfilledAt().toString() : null)
                .actualSearchRadiusKm(req.getActualSearchRadiusKm())
                .distance(distance)
                .myStatus(myStatus)
                .responsesCount(responsesCount)
                .donorsAccepted(donorsAccepted)
                .donorsCompleted(donorsCompleted)
                .createdAt(req.getCreatedAt() != null ? req.getCreatedAt().toString() : null)
                .updatedAt(req.getUpdatedAt() != null ? req.getUpdatedAt().toString() : null)
                .build();
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public DonorAchievementsResponse getDonorAchievements(Long userId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));

        List<DonorAchievement> earnedEntities = donorAchievementRepository.findByDonorId(donor.getId());
        Set<Long> earnedIds = earnedEntities.stream()
                .map(DonorAchievement::getAchievementId)
                .collect(Collectors.toSet());

        List<Achievement> allAchievements = achievementRepository.findAll();
        Set<Long> lookupIds = new HashSet<>(earnedIds);
        lookupIds.addAll(allAchievements.stream().map(Achievement::getId).toList());
        Map<Long, Achievement> achievementById = achievementRepository.findAllById(lookupIds).stream()
                .collect(Collectors.toMap(Achievement::getId, a -> a, (a, b) -> a));

        List<DonorAchievementView> earned = earnedEntities.stream()
                .map(da -> DonorAchievementView.of(da, AchievementView.of(achievementById.get(da.getAchievementId()))))
                .collect(Collectors.toList());

        List<AchievementView> locked = allAchievements.stream()
                .filter(a -> !earnedIds.contains(a.getId()))
                .map(AchievementView::of)
                .collect(Collectors.toList());

        return DonorAchievementsResponse.builder()
                .earned(earned)
                .locked(locked)
                .points(donor.getPoints() != null ? donor.getPoints() : 0)
                .level(donor.getLevel() != null ? donor.getLevel() : 1)
                .build();
    }
}
