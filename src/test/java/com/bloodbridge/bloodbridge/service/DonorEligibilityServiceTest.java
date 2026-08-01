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

class DonorEligibilityServiceTest extends AbstractIntegrationTest {

    @Autowired private DonorEligibilityService eligibilityService;
    @Autowired private DonorHealthProfileRepository healthProfileRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private EligibilityLogRepository eligibilityLogRepository;
    @Autowired private UserRepository userRepository;

    private Donor donor;

    @BeforeEach
    void setUp() {
        User donorUser = new User();
        donorUser.setName("Eligibility Test Donor");
        donorUser.setEmail("elig-test" + System.currentTimeMillis() + "@test.com");
        donorUser.setPassword("pass");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setIsActive(true);
        donorUser.setEmailVerifiedAt(java.time.LocalDateTime.now());
        donorUser.setLocale("en");
        donorUser = userRepository.save(donorUser);

        donor = new Donor();
        donor.setUserId(donorUser.getId());
        donor.setPoints(0);
        donor.setLevel(1);
        donor = donorRepository.save(donor);
    }

    @Test
    void shouldBeEligibleByDefault() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(false)
                .infection(false)
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isTrue();
        assertThat(updated.getNextEligibleDate()).isNull();
    }

    @Test
    void shouldMarkAsIneligibleForChronicDisease() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(true)
                .infection(false)
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isFalse();
        assertThat(updated.getNextEligibleDate()).isNull();
    }

    @Test
    void shouldMarkAsIneligibleForLowWeight() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(45)
                .height(170)
                .chronicDisease(false)
                .infection(false)
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isFalse();
    }

    @Test
    void shouldMarkAsIneligibleForLowHeight() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(130)
                .chronicDisease(false)
                .infection(false)
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isFalse();
    }

    @Test
    void shouldMarkAsIneligibleForRecentDonation() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(false)
                .infection(false)
                .lastDonationDate(LocalDate.now().minusDays(30))
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isFalse();
        assertThat(updated.getNextEligibleDate()).isAfter(LocalDate.now());
    }

    @Test
    void shouldCreateEligibilityLogWhenIneligible() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(true)
                .infection(false)
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        assertThat(eligibilityLogRepository.findByDonorIdOrderByCreatedAtDesc(donor.getId())).isNotEmpty();
    }

    @Test
    void shouldMakeEligibleAfterDonationCooldown() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(false)
                .infection(false)
                .lastDonationDate(LocalDate.now().minusDays(100))
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isTrue();
    }

    @Test
    void shouldBeIneligibleForActiveInfection() {
        DonorHealthProfile profile = DonorHealthProfile.builder()
                .donor(donor)
                .weight(75)
                .height(170)
                .chronicDisease(false)
                .infection(true)
                .build();
        profile = healthProfileRepository.save(profile);

        eligibilityService.calculateEligibility(profile);

        DonorHealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getIsEligible()).isFalse();
    }
}
