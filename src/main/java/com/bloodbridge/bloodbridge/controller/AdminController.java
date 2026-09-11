package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.dto.AdminBloodRequestView;
import com.bloodbridge.bloodbridge.dto.AdminDonorView;
import com.bloodbridge.bloodbridge.dto.AdminOrganizationView;
import com.bloodbridge.bloodbridge.dto.AdminResponseView;
import com.bloodbridge.bloodbridge.dto.AdminUserUpdateRequest;
import com.bloodbridge.bloodbridge.dto.AdminUserView;
import com.bloodbridge.bloodbridge.dto.AppSettingsResponse;
import com.bloodbridge.bloodbridge.entity.*;
import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.exception.ResourceNotFoundException;
import com.bloodbridge.bloodbridge.repository.*;
import com.bloodbridge.bloodbridge.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<Page<AdminUserView>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable).map(AdminUserView::of));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserView> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(AdminUserView.of(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserView> updateUser(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest userData) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (userData.getRole() != null) {
            if (userData.getRole() == UserRole.ADMIN || user.getRole() == UserRole.ADMIN) {
                throw new BusinessException("Admin accounts cannot be changed through this endpoint", HttpStatus.FORBIDDEN);
            }
            user.setRole(userData.getRole());
        }
        if (userData.getIsActive() != null) {
            if (!userData.getIsActive()
                    && (user.getRole() == UserRole.ADMIN || caller.getId().equals(user.getId()))) {
                throw new BusinessException("Admin accounts cannot be deactivated here, and you cannot deactivate yourself", HttpStatus.FORBIDDEN);
            }
            user.setIsActive(userData.getIsActive());
        }
        if (userData.getLocale() != null) user.setLocale(userData.getLocale());
        return ResponseEntity.ok(AdminUserView.of(userRepository.save(user)));
    }

    @GetMapping("/donors")
    public ResponseEntity<Page<AdminDonorView>> getDonors(Pageable pageable) {
        return ResponseEntity.ok(donorRepository.findAllForAdmin(pageable).map(AdminDonorView::of));
    }

    @GetMapping("/organizations")
    public ResponseEntity<Page<AdminOrganizationView>> getOrganizations(Pageable pageable) {
        return ResponseEntity.ok(organizationRepository.findAllForAdmin(pageable).map(AdminOrganizationView::of));
    }

    @PutMapping("/organizations/{id}/approve")
    public ResponseEntity<AdminOrganizationView> approveOrganization(@PathVariable Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setApprovalStatus(OrganizationStatus.APPROVED);
        return ResponseEntity.ok(AdminOrganizationView.of(organizationRepository.save(org)));
    }

    @PutMapping("/organizations/{id}/reject")
    public ResponseEntity<AdminOrganizationView> rejectOrganization(@PathVariable Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setApprovalStatus(OrganizationStatus.REJECTED);
        return ResponseEntity.ok(AdminOrganizationView.of(organizationRepository.save(org)));
    }

    @GetMapping("/blood-requests")
    public ResponseEntity<Page<AdminBloodRequestView>> getBloodRequests(Pageable pageable) {
        return ResponseEntity.ok(bloodRequestRepository.findAllForAdmin(pageable).map(AdminBloodRequestView::of));
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("usersCount", userRepository.count());
        overview.put("donorsCount", donorRepository.count());
        overview.put("organizationsCount", organizationRepository.count());
        overview.put("pendingOrganizationsCount",
                organizationRepository.findByApprovalStatusNotDeleted(OrganizationStatus.PENDING).size());
        overview.put("bloodRequestsCount", bloodRequestRepository.count());
        overview.put("pendingRequestsCount",
                bloodRequestRepository.countByStatus(com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus.PENDING));
        overview.put("activeRequestsCount",
                bloodRequestRepository.countByStatus(com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus.BROADCASTED));
        overview.put("completedRequestsCount",
                bloodRequestRepository.countByStatus(com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus.FULFILLED));
        overview.put("responsesCount", requestResponseRepository.count());
        overview.put("unreadContactMessagesCount",
                contactMessageRepository.findByStatusOrderByCreatedAtDesc("unread").size());
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/blood-requests/{id}")
    public ResponseEntity<AdminBloodRequestView> getBloodRequest(@PathVariable Long id) {
        BloodRequest request = bloodRequestRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));
        String orgName = organizationRepository.findById(request.getOrganizationId())
                .map(Organization::getOrgName).orElse(null);
        return ResponseEntity.ok(new AdminBloodRequestView(
                request.getId(), request.getOrganizationId(), orgName,
                request.getBloodType() != null ? request.getBloodType().getValue() : null,
                request.getUnitsNeeded(),
                request.getUrgencyLevel() != null ? request.getUrgencyLevel().getValue() : null,
                request.getStatus() != null ? request.getStatus().getValue() : null,
                request.getLocationAddress(), request.getBroadcastedAt(), request.getCreatedAt()));
    }

    @GetMapping("/responses")
    public ResponseEntity<Page<AdminResponseView>> getResponses(Pageable pageable) {
        return ResponseEntity.ok(requestResponseRepository.findAllForAdmin(pageable));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<Achievement>> getAchievements() {
        return ResponseEntity.ok(achievementRepository.findAll());
    }

    @PostMapping("/achievements")
    public ResponseEntity<Achievement> createAchievement(@Valid @RequestBody Achievement achievement) {
        // Never allow clients to pick ids/timestamps (would overwrite existing rows).
        achievement.setId(null);
        achievement.setCreatedAt(null);
        achievement.setUpdatedAt(null);
        return ResponseEntity.ok(achievementRepository.save(achievement));
    }

    @PutMapping("/achievements/{id}")
    public ResponseEntity<Achievement> updateAchievement(@PathVariable Long id, @Valid @RequestBody Achievement data) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found"));
        achievement.setName(data.getName());
        achievement.setDescription(data.getDescription());
        achievement.setPointsRewards(data.getPointsRewards());
        achievement.setBadgeIcon(data.getBadgeIcon());
        achievement.setBadgeType(data.getBadgeType());
        achievement.setCriteriaType(data.getCriteriaType());
        achievement.setCriteriaValue(data.getCriteriaValue());
        achievement.setDisplayOrder(data.getDisplayOrder());
        return ResponseEntity.ok(achievementRepository.save(achievement));
    }

    @DeleteMapping("/achievements/{id}")
    public ResponseEntity<Map<String, String>> deleteAchievement(@PathVariable Long id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found"));
        achievementRepository.delete(achievement);
        return ResponseEntity.ok(Map.of("message", "Achievement deleted"));
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
    public ResponseEntity<Announcement> createAnnouncement(@Valid @RequestBody Announcement announcement) {
        announcement.setId(null);
        announcement.setCreatedAt(null);
        announcement.setUpdatedAt(null);
        announcement.setDeletedAt(null);
        return ResponseEntity.ok(announcementRepository.save(announcement));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<Announcement> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody Announcement data) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        announcement.setTitleAr(data.getTitleAr());
        announcement.setTitleEn(data.getTitleEn());
        announcement.setContentAr(data.getContentAr());
        announcement.setContentEn(data.getContentEn());
        announcement.setImagePath(data.getImagePath());
        announcement.setIsPublished(data.getIsPublished());
        announcement.setPublishedAt(data.getPublishedAt());
        announcement.setEventDate(data.getEventDate());
        return ResponseEntity.ok(announcementRepository.save(announcement));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<Map<String, String>> deleteAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        announcementRepository.delete(announcement);
        return ResponseEntity.ok(Map.of("message", "Announcement deleted"));
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
        saveBoolean("general", "maintenanceMode", settings.get("maintenanceMode"));
        saveInt("general", "orgMaxRequestsPerDay", settings.get("orgMaxRequestsPerDay"), 1, 100);
        saveSetting("general", "siteName", settings.get("siteName"));
        saveSetting("general", "siteSlogan", settings.get("siteSlogan"));
        saveSetting("general", "siteLogo", settings.get("siteLogo"));
        saveSetting("general", "siteFavicon", settings.get("siteFavicon"));

        saveInt("eligibility", "minDonorAge", settings.get("minDonorAge"), 0, 120);
        saveInt("eligibility", "maxDonorAge", settings.get("maxDonorAge"), 0, 120);
        saveInt("eligibility", "minDonorWeight", settings.get("minDonorWeight"), 0, 500);
        saveInt("eligibility", "minDonorHeight", settings.get("minDonorHeight"), 0, 300);
        saveInt("eligibility", "minDaysBetweenDonations", settings.get("minDaysBetweenDonations"), 0, 3650);
        saveInt("eligibility", "minDaysAfterSurgery", settings.get("minDaysAfterSurgery"), 0, 3650);

        saveEmail("contact", "supportEmail", settings.get("supportEmail"));
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

    private void saveInt(String group, String name, Object value, int min, int max) {
        if (value == null) {
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException(name + " must be a number", HttpStatus.BAD_REQUEST);
        }
        if (parsed < min || parsed > max) {
            throw new BusinessException(name + " must be between " + min + " and " + max, HttpStatus.BAD_REQUEST);
        }
        settingsService.update(group, name, parsed);
    }

    private void saveBoolean(String group, String name, Object value) {
        if (value == null) {
            return;
        }
        if (!(value instanceof Boolean)) {
            throw new BusinessException(name + " must be true or false", HttpStatus.BAD_REQUEST);
        }
        settingsService.update(group, name, value);
    }

    private void saveEmail(String group, String name, Object value) {
        if (value == null) {
            return;
        }
        String email = String.valueOf(value).trim();
        if (!email.contains("@") || email.length() > 255) {
            throw new BusinessException(name + " must be a valid email", HttpStatus.BAD_REQUEST);
        }
        settingsService.update(group, name, email);
    }
}