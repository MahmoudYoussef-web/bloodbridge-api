package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.entity.ContactMessage;
import com.bloodbridge.bloodbridge.repository.ContactMessageRepository;
import com.bloodbridge.bloodbridge.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final ContactMessageRepository contactMessageRepository;
    private final RateLimitService rateLimitService;

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

    record ContactMessageRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String subject,
            @NotBlank String message,
            String phone
    ) {}
}
