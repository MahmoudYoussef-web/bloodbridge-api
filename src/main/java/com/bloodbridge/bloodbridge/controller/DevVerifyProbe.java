package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DEV-ONLY: auto-verifies a user's email so the verification gate can be exercised
 * in local H2 development. Loads only under the "h2" profile and double-guards at
 * runtime. Production has no mail integration yet, so this endpoint must never be
 * reachable there; replace it with a real email-verification flow (see README).
 */
@RestController
@RequestMapping("/v1/public")
@Profile("h2")
@RequiredArgsConstructor
public class DevVerifyProbe {

    private final UserRepository userRepository;
    private final Environment environment;

    @PostMapping("/verify-dev")
    public ResponseEntity<Map<String, String>> verify(@RequestBody Map<String, String> body) {
        if (!environment.acceptsProfiles(Profiles.of("h2"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dev endpoint not available");
        }
        String email = body.get("email");
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email));
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("verified", email));
    }
}
