package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.entity.Announcement;
import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.ContactMessage;
import com.bloodbridge.bloodbridge.entity.Governorate;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.repository.AnnouncementRepository;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
import com.bloodbridge.bloodbridge.repository.ContactMessageRepository;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.GovernorateRepository;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import com.bloodbridge.bloodbridge.service.RateLimitService;
import com.bloodbridge.bloodbridge.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final ContactMessageRepository contactMessageRepository;
    private final RateLimitService rateLimitService;
    private final GovernorateRepository governorateRepository;
    private final DonorRepository donorRepository;
    private final OrganizationRepository organizationRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final AnnouncementRepository announcementRepository;
    private final SettingsService settingsService;

    @PostMapping("/contact")
    public ResponseEntity<?> submitContact(
            @Valid @RequestBody ContactMessageRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        if (!rateLimitService.tryContactSubmission(ip)) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Too many requests. Please try again later.",
                            "retryAfter", "60s"));
        }

        ContactMessage message = ContactMessage.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .subject(request.subject())
                .message(request.message())
                .status("unread")
                .build();

        contactMessageRepository.save(message);
        return ResponseEntity.ok(Map.of("message", "Your message has been received. We will get back to you soon."));
    }

    @GetMapping("/governorates")
    public ResponseEntity<List<Governorate>> getGovernorates() {
        return ResponseEntity.ok(governorateRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        long donors = donorRepository.count();
        long orgs = organizationRepository.count();
        long completed = bloodRequestRepository.countByStatus(BloodRequestStatus.FULFILLED);
        long active = bloodRequestRepository.countByStatus(BloodRequestStatus.BROADCASTED);
        return ResponseEntity.ok(Map.of(
                "donorsCount", donors,
                "orgsCount", orgs,
                "livesSaved", completed,
                "activeRequests", active));
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getPublicSettings() {
        return ResponseEntity.ok(Map.of(
                "siteName", settingsService.getString("general", "siteName", "BloodBridge"),
                "siteSlogan", settingsService.getString("general", "siteSlogan", ""),
                "supportEmail", settingsService.getString("contact", "supportEmail", ""),
                "supportPhone", settingsService.getString("contact", "supportPhone", "")));
    }

    /**
     * Public-safe live feed of broadcasted requests. No donor data, no QR codes,
     * no internal ids beyond the request itself — safe to render on the homepage.
     */
    @GetMapping("/urgent-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UrgentRequestView>> getUrgentRequests() {
        // Bounded fetch: only the most recent candidates are ranked in memory.
        List<BloodRequest> active = bloodRequestRepository.findActiveByStatus(
                BloodRequestStatus.BROADCASTED, PageRequest.of(0, 100)).getContent();
        List<UrgentRequestView> views = active.stream()
                .sorted(Comparator
                        .comparing((BloodRequest r) -> r.getUrgencyLevel() != UrgencyLevel.CRITICAL)
                        .thenComparing((BloodRequest r) -> r.getBroadcastedAt(),
                                Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()).reversed()))
                .limit(6)
                .map(r -> new UrgentRequestView(
                        r.getId(),
                        r.getBloodType(),
                        r.getUnitsNeeded(),
                        r.getUrgencyLevel(),
                        r.getLocationAddress(),
                        r.getOrganization() != null ? r.getOrganization().getOrgName() : null,
                        r.getBroadcastedAt()))
                .toList();
        return ResponseEntity.ok(views);
    }

    record UrgentRequestView(
            Long id,
            BloodType bloodType,
            Integer unitsNeeded,
            UrgencyLevel urgencyLevel,
            String locationAddress,
            String organizationName,
            LocalDateTime broadcastedAt
    ) {}

    @GetMapping("/announcements")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AnnouncementView>> getAnnouncements() {
        List<AnnouncementView> views = announcementRepository
                .findTop6PublishedOrderByPublishedAtDesc(PageRequest.of(0, 6))
                .stream()
                .map(a -> new AnnouncementView(
                        a.getId(),
                        a.getTitleAr(),
                        a.getTitleEn(),
                        a.getContentAr(),
                        a.getContentEn(),
                        a.getPublishedAt(),
                        a.getEventDate()))
                .toList();
        return ResponseEntity.ok(views);
    }

    record AnnouncementView(
            Long id,
            String titleAr,
            String titleEn,
            String contentAr,
            String contentEn,
            LocalDateTime publishedAt,
            LocalDateTime eventDate
    ) {}

    record ContactMessageRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String subject,
            @NotBlank String message,
            String phone
    ) {}
}
