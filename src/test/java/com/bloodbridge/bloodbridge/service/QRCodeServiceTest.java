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

class QRCodeServiceTest extends AbstractIntegrationTest {

    @Autowired private QRCodeService qrCodeService;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private RequestResponseRepository requestResponseRepository;
    @Autowired private UserRepository userRepository;

    private Organization org;
    private BloodRequest request;
    private RequestResponse response;

    @BeforeEach
    void setUp() {
        User orgUser = new User();
        orgUser.setName("QR Org User");
        orgUser.setEmail("qr-org-user" + System.currentTimeMillis() + "@test.com");
        orgUser.setPassword("pass");
        orgUser.setRole(UserRole.ORGANIZATION);
        orgUser.setIsActive(true);
        orgUser.setEmailVerifiedAt(java.time.LocalDateTime.now());
        orgUser.setLocale("en");
        orgUser = userRepository.save(orgUser);

        Organization savedOrg = new Organization();
        savedOrg.setOrgName("Test QR Org");
        savedOrg.setSlug("qr-org-" + System.currentTimeMillis());
        savedOrg.setContactEmail("qr@test.com");
        savedOrg.setContactPhone("1234567890");
        savedOrg.setUserId(orgUser.getId());
        savedOrg.setApprovalStatus(OrganizationStatus.APPROVED);
        org = organizationRepository.save(savedOrg);

        BloodRequest req = new BloodRequest();
        req.setOrganization(org);
        req.setOrganizationId(org.getId());
        req.setStatus(BloodRequestStatus.BROADCASTED);
        req.setBloodType(BloodType.A_POSITIVE);
        req.setUnitsNeeded(2);
        req.setUrgencyLevel(UrgencyLevel.NORMAL);
        req.setSearchRadiusKm(10);
        request = bloodRequestRepository.save(req);
    }

    @Test
    void shouldGenerateRandomToken() {
        String token = qrCodeService.generate();
        assertThat(token).isNotNull().hasSize(32);
    }

    @Test
    void shouldGenerateUniqueTokens() {
        String t1 = qrCodeService.generate();
        String t2 = qrCodeService.generate();
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void shouldValidateCorrectToken() {
        String token = qrCodeService.generate();
        LocalDateTime future = LocalDateTime.now().plusDays(6);
        assertThat(qrCodeService.validate(token, token, future)).isTrue();
    }

    @Test
    void shouldRejectWrongToken() {
        String token = qrCodeService.generate();
        LocalDateTime future = LocalDateTime.now().plusDays(6);
        assertThat(qrCodeService.validate("wrong", token, future)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = qrCodeService.generate();
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        assertThat(qrCodeService.validate(token, token, past)).isFalse();
    }

    @Test
    void shouldRejectNullToken() {
        assertThat(qrCodeService.validate(null, "stored", LocalDateTime.now().plusDays(1))).isFalse();
        assertThat(qrCodeService.validate("token", null, LocalDateTime.now().plusDays(1))).isFalse();
    }

    @Test
    void shouldGenerateQrImage() {
        String token = qrCodeService.generate();
        byte[] image = qrCodeService.generateQrImage(token);
        assertThat(image).isNotNull().isNotEmpty();
    }

    @Test
    void shouldValidateWithOrganization() {
        String token = qrCodeService.generate();
        LocalDateTime future = LocalDateTime.now().plusDays(6);
        assertThat(qrCodeService.validate(token, token, future, org.getId(), org.getId())).isTrue();
    }

    @Test
    void shouldRejectWithWrongOrganization() {
        String token = qrCodeService.generate();
        LocalDateTime future = LocalDateTime.now().plusDays(6);
        assertThat(qrCodeService.validate(token, token, future, 999L, org.getId())).isFalse();
    }
}
