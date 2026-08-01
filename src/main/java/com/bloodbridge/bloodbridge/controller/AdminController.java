package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.dto.AppSettingsResponse;
import com.bloodbridge.bloodbridge.entity.*;
import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.exception.ResourceNotFoundException;
import com.bloodbridge.bloodbridge.repository.*;
import com.bloodbridge.bloodbridge.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final OrganizationRepository organizationRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final DonorHealthProfileRepository healthProfileRepository;
    private final AchievementRepository achievementRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final AnnouncementRepository announcementRepository;
    private final SettingsService settingsService;

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userData) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (userData.getRole() != null) {
            if (userData.getRole() == UserRole.ADMIN) {
                throw new BusinessException("Cannot assign ADMIN role through this endpoint", HttpStatus.FORBIDDEN);
            }
            user.setRole(userData.getRole());
        }
        if (userData.getIsActive() != null) user.setIsActive(userData.getIsActive());
        if (userData.getLocale() != null) user.setLocale(userData.getLocale());
        return ResponseEntity.ok(userRepository.save(user));
    }

    @GetMapping("/donors")
    public ResponseEntity<Page<Donor>> getDonors(Pageable pageable) {
        return ResponseEntity.ok(donorRepository.findAll(pageable));
    }

    @GetMapping("/organizations")
    public ResponseEntity<Page<Organization>> getOrganizations(Pageable pageable) {
        return ResponseEntity.ok(organizationRepository.findAll(pageable));
    }

    @PutMapping("/organizations/{id}/approve")
    public ResponseEntity<Organization> approveOrganization(@PathVariable Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setApprovalStatus(OrganizationStatus.APPROVED);
        return ResponseEntity.ok(organizationRepository.save(org));
    }

    @PutMapping("/organizations/{id}/reject")
    public ResponseEntity<Organization> rejectOrganization(@PathVariable Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setApprovalStatus(OrganizationStatus.REJECTED);
        return ResponseEntity.ok(organizationRepository.save(org));
    }

    @GetMapping("/blood-requests")
    public ResponseEntity<Page<BloodRequest>> getBloodRequests(Pageable pageable) {
        return ResponseEntity.ok(bloodRequestRepository.findAll(pageable));
    }

    @GetMapping("/blood-requests/{id}")
    public ResponseEntity<BloodRequest> getBloodRequest(@PathVariable Long id) {
        return ResponseEntity.ok(bloodRequestRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found")));
    }

    @GetMapping("/responses")
    public ResponseEntity<Page<RequestResponse>> getResponses(Pageable pageable) {
        return ResponseEntity.ok(requestResponseRepository.findAll(pageable));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<Achievement>> getAchievements() {
        return ResponseEntity.ok(achievementRepository.findAll());
    }

    @PostMapping("/achievements")
    public ResponseEntity<Achievement> createAchievement(@RequestBody Achievement achievement) {
        return ResponseEntity.ok(achievementRepository.save(achievement));
    }

    @GetMapping("/contact-messages")
    public ResponseEntity<Page<ContactMessage>> getContactMessages(Pageable pageable) {
        return ResponseEntity.ok(contactMessageRepository.findAll(pageable));
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<Announcement>> getAnnouncements() {
        return ResponseEntity.ok(announcementRepository.findAll());
    }

    @PostMapping("/announcements")
    public ResponseEntity<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        return ResponseEntity.ok(announcementRepository.save(announcement));
    }

    @GetMapping("/settings")
    public ResponseEntity<AppSettingsResponse> getSettings() {
        AppSettingsResponse settings = AppSettingsResponse.builder()
                .siteName(readLocalizedSetting("general", "siteName"))
                .siteSlogan(readLocalizedSetting("general", "siteSlogan"))
                .supportEmail(settingsService.getString("contact", "supportEmail", null))
                .supportPhone(settingsService.getString("contact", "supportPhone", null))
                .address(readLocalizedSetting("contact", "address"))
                .socialLinks(readJsonMap("contact", "socialLinks"))
                .minDonorAge(settingsService.getInt("eligibility", "minDonorAge", 18))
                .maxDonorAge(settingsService.getInt("eligibility", "maxDonorAge", 65))
                .minDonorWeight(settingsService.getInt("eligibility", "minDonorWeight", 50))
                .minDonorHeight(settingsService.getInt("eligibility", "minDonorHeight", 140))
                .minDaysBetweenDonations(settingsService.getInt("eligibility", "minDaysBetweenDonations", 90))
                .minDaysAfterSurgery(settingsService.getInt("eligibility", "minDaysAfterSurgery", 28))
                .orgMaxRequestsPerDay(settingsService.getInt("general", "orgMaxRequestsPerDay", 5))
                .maintenanceMode(settingsService.getBoolean("general", "maintenanceMode", false))
                .enableContactMessages(settingsService.getBoolean("general", "enableContactMessages", true))
                .loginTitle(readLocalizedSetting("content", "loginTitle"))
                .loginSubtitle(readLocalizedSetting("content", "loginSubtitle"))
                .signupTitle(readLocalizedSetting("content", "signupTitle"))
                .signupSubtitle(readLocalizedSetting("content", "signupSubtitle"))
                .heroTitle(readLocalizedSetting("content", "heroTitle"))
                .heroSubtitle(readLocalizedSetting("content", "heroSubtitle"))
                .siteLogo(settingsService.getString("general", "siteLogo", null))
                .siteFavicon(settingsService.getString("general", "siteFavicon", null))
                .build();
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<AppSettingsResponse> updateSettings(@RequestBody Map<String, Object> settings) {
        saveSetting("general", "maintenanceMode", settings.get("maintenanceMode"));
        saveSetting("general", "orgMaxRequestsPerDay", settings.get("orgMaxRequestsPerDay"));
        saveSetting("general", "siteName", settings.get("siteName"));
        saveSetting("general", "siteSlogan", settings.get("siteSlogan"));
        saveSetting("general", "siteLogo", settings.get("siteLogo"));
        saveSetting("general", "siteFavicon", settings.get("siteFavicon"));

        saveSetting("eligibility", "minDonorAge", settings.get("minDonorAge"));
        saveSetting("eligibility", "maxDonorAge", settings.get("maxDonorAge"));
        saveSetting("eligibility", "minDonorWeight", settings.get("minDonorWeight"));
        saveSetting("eligibility", "minDonorHeight", settings.get("minDonorHeight"));
        saveSetting("eligibility", "minDaysBetweenDonations", settings.get("minDaysBetweenDonations"));
        saveSetting("eligibility", "minDaysAfterSurgery", settings.get("minDaysAfterSurgery"));

        saveSetting("contact", "supportEmail", settings.get("supportEmail"));
        saveSetting("contact", "supportPhone", settings.get("supportPhone"));
        saveSetting("contact", "address", settings.get("address"));
        saveSetting("contact", "socialLinks", settings.get("socialLinks"));

        saveSetting("content", "loginTitle", settings.get("loginTitle"));
        saveSetting("content", "loginSubtitle", settings.get("loginSubtitle"));
        saveSetting("content", "signupTitle", settings.get("signupTitle"));
        saveSetting("content", "signupSubtitle", settings.get("signupSubtitle"));
        saveSetting("content", "heroTitle", settings.get("heroTitle"));
        saveSetting("content", "heroSubtitle", settings.get("heroSubtitle"));

        settingsService.loadCache();
        return getSettings();
    }

    private Map<String, String> readLocalizedSetting(String group, String name) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = settingsService.get(group, name);
            if (node == null) return new HashMap<>();
            Map<String, String> result = new HashMap<>();
            if (node.isObject()) {
                node.fieldNames().forEachRemaining(f -> result.put(f, node.get(f).asText()));
            }
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, String> readJsonMap(String group, String name) {
        return readLocalizedSetting(group, name);
    }

    private void saveSetting(String group, String name, Object value) {
        if (value != null) {
            settingsService.update(group, name, value);
        }
    }
}