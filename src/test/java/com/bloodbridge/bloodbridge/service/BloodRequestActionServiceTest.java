package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.AbstractIntegrationTest;
import com.bloodbridge.bloodbridge.entity.*;
import com.bloodbridge.bloodbridge.enumtype.*;
import com.bloodbridge.bloodbridge.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BloodRequestActionServiceTest extends AbstractIntegrationTest {

    @Autowired private BloodRequestActionService actionService;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RequestResponseRepository requestResponseRepository;
    @Autowired private DonorHealthProfileRepository healthProfileRepository;

    private Organization org;
    private BloodRequest request;
    private User orgUser;
    private User donorUser;
    private Donor donor;

    @BeforeEach
    void setUp() {
        orgUser = new User();
        orgUser.setName("Test Org User");
        orgUser.setEmail("org" + System.currentTimeMillis() + "@test.com");
        orgUser.setPassword("pass");
        orgUser.setRole(UserRole.ORGANIZATION);
        orgUser.setIsActive(true);
        orgUser.setEmailVerifiedAt(LocalDateTime.now());
        orgUser.setLocale("en");
        orgUser = userRepository.save(orgUser);

        Organization savedOrg = new Organization();
        savedOrg.setOrgName("Test Action Org");
        savedOrg.setSlug("act-org-" + System.currentTimeMillis());
        savedOrg.setContactEmail("act@test.com");
        savedOrg.setContactPhone("1112223333");
        savedOrg.setUserId(orgUser.getId());
        savedOrg.setApprovalStatus(OrganizationStatus.APPROVED);
        org = organizationRepository.save(savedOrg);

        BloodRequest req = new BloodRequest();
        req.setOrganization(org);
        req.setOrganizationId(org.getId());
        req.setStatus(BloodRequestStatus.BROADCASTED);
        req.setBloodType(BloodType.O_NEGATIVE);
        req.setUnitsNeeded(2);
        req.setUrgencyLevel(UrgencyLevel.NORMAL);
        req.setSearchRadiusKm(10);
        req.setLat(31.5);
        req.setLng(34.4667);
        request = bloodRequestRepository.save(req);

        donorUser = new User();
        donorUser.setName("Test Donor");
        donorUser.setEmail("donor" + System.currentTimeMillis() + "@test.com");
        donorUser.setPassword("pass");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setIsActive(true);
        donorUser.setEmailVerifiedAt(LocalDateTime.now());
        donorUser.setLocale("en");
        donorUser = userRepository.save(donorUser);

        donor = new Donor();
        donor.setUserId(donorUser.getId());
        donor.setPoints(0);
        donor.setLevel(1);
        donor = donorRepository.save(donor);

        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(false)
                .infection(false)
                .isEligible(true)
                .build();
        healthProfileRepository.save(profile);
    }

    @Test
    void shouldAcceptBloodRequest() {
        RequestResponse response = actionService.accept(donorUser, request.getId(), 31.5, 34.47);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RequestResponseStatus.PENDING);
        assertThat(response.getBloodRequestId()).isEqualTo(request.getId());
        assertThat(response.getDonorId()).isEqualTo(donor.getId());
        assertThat(response.getVerificationQrCode()).isNotNull().hasSize(32);
        assertThat(response.getQrCodeExpiresAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateAcceptance() {
        actionService.accept(donorUser, request.getId(), null, null);
        assertThrows(RuntimeException.class, () ->
                actionService.accept(donorUser, request.getId(), null, null));
    }

    @Test
    void shouldRejectAcceptForInactiveRequest() {
        request.setStatus(BloodRequestStatus.EXPIRED);
        bloodRequestRepository.save(request);
        assertThrows(RuntimeException.class, () ->
                actionService.accept(donorUser, request.getId(), null, null));
    }

    @Test
    void shouldDeclineResponse() {
        actionService.accept(donorUser, request.getId(), null, null);
        RequestResponse declined = actionService.decline(donorUser, request.getId(), "Medical reason");
        assertThat(declined.getStatus()).isEqualTo(RequestResponseStatus.DECLINED);
        assertThat(declined.getDeclineReason()).isEqualTo("Medical reason");
    }

    @Test
    void shouldIgnoreResponse() {
        actionService.accept(donorUser, request.getId(), null, null);
        RequestResponse ignored = actionService.ignore(donorUser, request.getId());
        assertThat(ignored.getStatus()).isEqualTo(RequestResponseStatus.IGNORED);
    }

    @Test
    void shouldConfirmAdmission() {
        RequestResponse response = actionService.accept(donorUser, request.getId(), null, null);
        RequestResponse confirmed = actionService.confirmAdmission(response.getVerificationQrCode(), org);
        assertThat(confirmed.getStatus()).isEqualTo(RequestResponseStatus.ACCEPTED);
        assertThat(confirmed.getVerifiedAt()).isNotNull();
    }

    @Test
    void shouldRejectAdmissionForWrongOrg() {
        RequestResponse response = actionService.accept(donorUser, request.getId(), null, null);

        User otherOrgUser = new User();
        otherOrgUser.setName("Other Org User");
        otherOrgUser.setEmail("other" + System.currentTimeMillis() + "@test.com");
        otherOrgUser.setPassword("pass");
        otherOrgUser.setRole(UserRole.ORGANIZATION);
        otherOrgUser.setIsActive(true);
        otherOrgUser.setEmailVerifiedAt(LocalDateTime.now());
        otherOrgUser.setLocale("en");
        otherOrgUser = userRepository.save(otherOrgUser);

        Organization otherOrg = new Organization();
        otherOrg.setOrgName("Other Org");
        otherOrg.setSlug("other-act-" + System.currentTimeMillis());
        otherOrg.setContactEmail("otheract@test.com");
        otherOrg.setContactPhone("4445556666");
        otherOrg.setUserId(otherOrgUser.getId());
        otherOrg.setApprovalStatus(OrganizationStatus.APPROVED);
        Organization savedOther = organizationRepository.save(otherOrg);

        assertThrows(RuntimeException.class, () ->
                actionService.confirmAdmission(response.getVerificationQrCode(), savedOther));
    }

    @Test
    void shouldCompleteResponse() {
        RequestResponse response = actionService.accept(donorUser, request.getId(), null, null);
        RequestResponse confirmed = actionService.confirmAdmission(response.getVerificationQrCode(), org);

        RequestResponse completed = actionService.complete(orgUser, confirmed.getId());
        assertThat(completed.getStatus()).isEqualTo(RequestResponseStatus.COMPLETED);

        DonorHealthProfile profile = healthProfileRepository.findByDonorId(donor.getId()).orElseThrow();
        assertThat(profile.getLastDonationDate()).isNotNull().isEqualTo(LocalDate.now());
    }

    @Test
    void shouldRejectCompleteForWrongOrg() {
        RequestResponse response = actionService.accept(donorUser, request.getId(), null, null);
        RequestResponse confirmed = actionService.confirmAdmission(response.getVerificationQrCode(), org);

        User otherOrgUser = new User();
        otherOrgUser.setName("Other Complete Org User");
        otherOrgUser.setEmail("other-complete" + System.currentTimeMillis() + "@test.com");
        otherOrgUser.setPassword("pass");
        otherOrgUser.setRole(UserRole.ORGANIZATION);
        otherOrgUser.setIsActive(true);
        otherOrgUser.setEmailVerifiedAt(LocalDateTime.now());
        otherOrgUser.setLocale("en");
        User savedOtherOrgUser = userRepository.save(otherOrgUser);

        Organization otherOrg = new Organization();
        otherOrg.setOrgName("Other Complete Org");
        otherOrg.setSlug("other-complete-" + System.currentTimeMillis());
        otherOrg.setContactEmail("othercomplete@test.com");
        otherOrg.setContactPhone("7778889999");
        otherOrg.setUserId(savedOtherOrgUser.getId());
        otherOrg.setApprovalStatus(OrganizationStatus.APPROVED);
        organizationRepository.save(otherOrg);

        assertThrows(RuntimeException.class, () ->
                actionService.complete(savedOtherOrgUser, confirmed.getId()));
    }

    @Test
    void shouldRejectCompleteNonAcceptedResponse() {
        assertThrows(RuntimeException.class, () ->
                actionService.complete(donorUser, 99999L));
    }

    @Test
    void shouldRejectAcceptWhenDonorIneligible() {
        DonorHealthProfile profile = healthProfileRepository.findByDonorId(donor.getId()).orElseThrow();
        profile.setChronicDisease(true);
        profile.setIsEligible(false);
        healthProfileRepository.save(profile);

        assertThrows(RuntimeException.class, () ->
                actionService.accept(donorUser, request.getId(), null, null));
    }

    @Test
    void shouldCalculateDistanceOnAccept() {
        RequestResponse response = actionService.accept(donorUser, request.getId(), 31.51, 34.47);
        assertThat(response.getDistance()).isNotNull().isPositive();
    }
}
