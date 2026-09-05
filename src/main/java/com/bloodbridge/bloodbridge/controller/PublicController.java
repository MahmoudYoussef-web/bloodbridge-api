package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.entity.ContactMessage;
import com.bloodbridge.bloodbridge.entity.Governorate;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    record ContactMessageRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String subject,
            @NotBlank String message,
            String phone
    ) {}
}
