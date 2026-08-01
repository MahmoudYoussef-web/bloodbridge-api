package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.AbstractIntegrationTest;
import com.bloodbridge.bloodbridge.entity.*;
import com.bloodbridge.bloodbridge.enumtype.*;
import com.bloodbridge.bloodbridge.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BloodRequestBroadcastServiceTest extends AbstractIntegrationTest {

    @Autowired private BloodRequestBroadcastService broadcastService;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorHealthProfileRepository healthProfileRepository;
    @Autowired private RequestResponseRepository requestResponseRepository;

    private Organization org;
    private BloodRequest request;
    private Donor donor1;
    private Donor donor2;

    @BeforeEach
    void setUp() {
        User orgUser = new User();
        orgUser.setName("Broadcast Org User");
        orgUser.setEmail("bc-org-user" + System.currentTimeMillis() + "@test.com");
        orgUser.setPassword("pass");
        orgUser.setRole(UserRole.ORGANIZATION);
        orgUser.setIsActive(true);
        orgUser.setEmailVerifiedAt(LocalDateTime.now());
        orgUser.setLocale("en");
        orgUser = userRepository.save(orgUser);

        Organization savedOrg = new Organization();
        savedOrg.setOrgName("Broadcast Org");
        savedOrg.setSlug("bc-org-" + System.currentTimeMillis());
        savedOrg.setContactEmail("bc@test.com");
        savedOrg.setContactPhone("1112223333");
        savedOrg.setUserId(orgUser.getId());
        savedOrg.setApprovalStatus(OrganizationStatus.APPROVED);
        org = organizationRepository.save(savedOrg);

        BloodRequest req = new BloodRequest();
        req.setOrganization(org);
        req.setOrganizationId(org.getId());
        req.setStatus(BloodRequestStatus.PENDING);
        req.setBloodType(BloodType.A_POSITIVE);
        req.setUnitsNeeded(2);
        req.setUrgencyLevel(UrgencyLevel.NORMAL);
        req.setSearchRadiusKm(10);
        req.setLat(31.5);
        req.setLng(34.4667);
        request = bloodRequestRepository.save(req);

        donor1 = createDonor("donor1", 31.50, 34.47, BloodType.A_POSITIVE, false);
        donor2 = createDonor("donor2", 31.52, 34.46, BloodType.O_POSITIVE, false);

        donorRepository.flush();
        healthProfileRepository.flush();
    }

    private Donor createDonor(String emailPrefix, double lat, double lng, BloodType bloodType, boolean ineligible) {
        User user = new User();
        user.setName(emailPrefix);
        user.setEmail(emailPrefix + System.currentTimeMillis() + "@test.com");
        user.setPassword("pass");
        user.setRole(UserRole.DONOR);
        user.setIsActive(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setLocale("en");
        user = userRepository.save(user);

        Donor donor = new Donor();
        donor.setUserId(user.getId());
        donor.setLat(lat);
        donor.setLng(lng);
        donor.setPoints(10);
        donor.setLevel(1);
        donor = donorRepository.save(donor);

        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .bloodType(bloodType)
                .chronicDisease(ineligible)
                .infection(false)
                .isEligible(!ineligible)
                .totalDonations(3)
                .build();
        healthProfileRepository.save(profile);

        donor.setHealthProfile(profile);
        donorRepository.save(donor);

        return donor;
    }

    @Test
    void shouldBroadcastAndChangeStatus() {
        broadcastService.broadcast(request);

        BloodRequest updated = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BloodRequestStatus.BROADCASTED);
        assertThat(updated.getBroadcastedAt()).isNotNull();
    }

    @Test
    void shouldSetActualSearchRadius() {
        broadcastService.broadcast(request);

        BloodRequest updated = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getActualSearchRadiusKm()).isGreaterThan(0);
    }

    @Test
    void shouldSkipRebroadcastIfAlreadyBroadcasted() {
        broadcastService.broadcast(request);
        broadcastService.broadcast(request);

        BloodRequest updated = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BloodRequestStatus.BROADCASTED);
    }

    @Test
    void shouldCreateResponsesForEligibleDonors() {
        int selected = broadcastService.broadcast(request);

        assertThat(selected).isGreaterThan(0);

        var responses = requestResponseRepository.findByBloodRequestId(request.getId());
        assertThat(responses).isNotEmpty();
    }

    @Test
    void shouldExcludeDonorsWithChronicDisease() {
        Donor sickDonor = createDonor("sick", 31.50, 34.47, BloodType.A_POSITIVE, true);

        broadcastService.broadcast(request);

        var responses = requestResponseRepository.findByBloodRequestId(request.getId());
        boolean sickFound = responses.stream().anyMatch(r -> r.getDonorId().equals(sickDonor.getId()));
        assertThat(sickFound).isFalse();
    }

    @Test
    void shouldHandleRequestWithNoCloseDonors() {
        BloodRequest farRequest = new BloodRequest();
        farRequest.setOrganization(org);
        farRequest.setOrganizationId(org.getId());
        farRequest.setStatus(BloodRequestStatus.PENDING);
        farRequest.setBloodType(BloodType.AB_NEGATIVE);
        farRequest.setUnitsNeeded(1);
        farRequest.setUrgencyLevel(UrgencyLevel.NORMAL);
        farRequest.setSearchRadiusKm(1);
        farRequest.setLat(32.0);
        farRequest.setLng(35.0);
        farRequest = bloodRequestRepository.save(farRequest);

        broadcastService.broadcast(farRequest);

        BloodRequest updated = bloodRequestRepository.findById(farRequest.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BloodRequestStatus.BROADCASTED);
    }

    @Test
    void shouldHandleCriticalUrgencyWithExpandedRadius() {
        BloodRequest criticalRequest = new BloodRequest();
        criticalRequest.setOrganization(org);
        criticalRequest.setOrganizationId(org.getId());
        criticalRequest.setStatus(BloodRequestStatus.PENDING);
        criticalRequest.setBloodType(BloodType.A_POSITIVE);
        criticalRequest.setUnitsNeeded(3);
        criticalRequest.setUrgencyLevel(UrgencyLevel.CRITICAL);
        criticalRequest.setSearchRadiusKm(10);
        criticalRequest.setLat(31.5);
        criticalRequest.setLng(34.4667);
        criticalRequest = bloodRequestRepository.save(criticalRequest);

        broadcastService.broadcast(criticalRequest);

        BloodRequest updated = bloodRequestRepository.findById(criticalRequest.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BloodRequestStatus.BROADCASTED);
    }
}
