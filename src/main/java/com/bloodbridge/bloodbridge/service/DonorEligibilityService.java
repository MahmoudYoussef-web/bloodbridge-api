package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorHealthProfile;
import com.bloodbridge.bloodbridge.entity.EligibilityLog;
import com.bloodbridge.bloodbridge.repository.DonorHealthProfileRepository;
import com.bloodbridge.bloodbridge.repository.EligibilityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonorEligibilityService {

    private final DonorHealthProfileRepository healthProfileRepository;
    private final EligibilityLogRepository eligibilityLogRepository;
    private final SettingsService settingsService;

    private int getMinWeight() { return settingsService.getInt("general", "min_donor_weight", 50); }
    private int getMinHeight() { return settingsService.getInt("general", "min_donor_height", 140); }
    private int getMinDaysBetweenDonations() { return settingsService.getInt("general", "min_days_between_donations", 90); }
    private int getMinDaysAfterSurgery() { return settingsService.getInt("general", "min_days_after_surgery", 28); }

    @Transactional
    public void calculateEligibility(DonorHealthProfile profile) {
        boolean wasEligible = Boolean.TRUE.equals(profile.getIsEligible());
        boolean isEligible = true;
        LocalDate nextEligibleDate = null;
        int minWeight = getMinWeight();
        int minHeight = getMinHeight();
        int minDaysDonations = getMinDaysBetweenDonations();
        int minDaysSurgery = getMinDaysAfterSurgery();

        if (Boolean.TRUE.equals(profile.getChronicDisease())) {
            isEligible = false;
            nextEligibleDate = null;
        } else if (profile.getWeight() != null && profile.getWeight() < minWeight) {
            isEligible = false;
            nextEligibleDate = null;
        } else if (profile.getHeight() != null && profile.getHeight() < minHeight) {
            isEligible = false;
            nextEligibleDate = null;
        } else if (profile.getLastDonationDate() != null) {
            long daysSinceDonation = java.time.temporal.ChronoUnit.DAYS.between(
                    profile.getLastDonationDate(), LocalDate.now());
            if (daysSinceDonation < minDaysDonations) {
                isEligible = false;
                nextEligibleDate = profile.getLastDonationDate().plusDays(minDaysDonations);
            }
        } else if (Boolean.TRUE.equals(profile.getInfection())) {
            isEligible = false;
            nextEligibleDate = LocalDate.now().plusDays(14);
        } else if (profile.getSurgeryDate() != null) {
            long daysSinceSurgery = java.time.temporal.ChronoUnit.DAYS.between(
                    profile.getSurgeryDate(), LocalDate.now());
            if (daysSinceSurgery < minDaysSurgery) {
                isEligible = false;
                nextEligibleDate = profile.getSurgeryDate().plusDays(minDaysSurgery);
            }
        }

        boolean eligibilityChanged = wasEligible != isEligible
                || (nextEligibleDate != null
                && profile.getNextEligibleDate() != null
                && !nextEligibleDate.equals(profile.getNextEligibleDate()));

        profile.setIsEligible(isEligible);
        profile.setNextEligibleDate(nextEligibleDate);

        healthProfileRepository.save(profile);

        if (!isEligible) {
            EligibilityLog logEntry = new EligibilityLog();
            logEntry.setDonorId(profile.getDonor().getId());
            logEntry.setIsEligible(isEligible);
            logEntry.setIsPermanent(Boolean.TRUE.equals(profile.getChronicDisease()));
            logEntry.setCheckType(1);
            logEntry.setRejectionReason(buildIneligibilityReason(profile, minWeight, minHeight, minDaysDonations, minDaysSurgery));
            eligibilityLogRepository.save(logEntry);
        }

        if (eligibilityChanged) {
            log.info("Donor {} eligibility changed: wasEligible={}, isEligible={}, nextEligible={}",
                    profile.getDonor().getId(), wasEligible, isEligible, nextEligibleDate);
        }
    }

    private String buildIneligibilityReason(DonorHealthProfile profile, int minWeight, int minHeight,
                                             int minDaysDonations, int minDaysSurgery) {
        if (Boolean.TRUE.equals(profile.getChronicDisease())) {
            return "Permanent ineligibility due to chronic disease";
        }
        if (profile.getWeight() != null && profile.getWeight() < minWeight) {
            return "Weight below minimum threshold (" + minWeight + " kg)";
        }
        if (profile.getHeight() != null && profile.getHeight() < minHeight) {
            return "Height below minimum threshold (" + minHeight + " cm)";
        }
        if (profile.getLastDonationDate() != null) {
            long daysSinceDonation = java.time.temporal.ChronoUnit.DAYS.between(
                    profile.getLastDonationDate(), LocalDate.now());
            return "Less than " + minDaysDonations + " days since last donation (" + daysSinceDonation + " days)";
        }
        if (Boolean.TRUE.equals(profile.getInfection())) {
            return "Active infection";
        }
        if (profile.getSurgeryDate() != null) {
            long daysSinceSurgery = java.time.temporal.ChronoUnit.DAYS.between(
                    profile.getSurgeryDate(), LocalDate.now());
            return "Less than " + minDaysSurgery + " days since surgery (" + daysSinceSurgery + " days)";
        }
        return "Unknown eligibility reason";
    }
}