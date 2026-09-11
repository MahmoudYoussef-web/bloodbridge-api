package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Creates (or repairs) the initial ADMIN account from environment variables.
 *
 * Set {@code ADMIN_EMAIL} and {@code ADMIN_PASSWORD} (min 8 chars) on first
 * deploy. Without them no admin can exist, because self-registration as ADMIN
 * is rejected and nothing else seeds one. The runner is a no-op when the
 * variables are absent, and never overwrites an existing password.
 */
@Component
@Order(1000)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_NAME:BloodBridge Admin}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set, skipping admin bootstrap");
            return;
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters");
        }
        String email = adminEmail.trim().toLowerCase();
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            boolean fixed = false;
            if (user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.ADMIN);
                fixed = true;
            }
            if (user.getIsActive() == null || !user.getIsActive()) {
                user.setIsActive(true);
                fixed = true;
            }
            if (user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(LocalDateTime.now());
                fixed = true;
            }
            if (fixed) {
                userRepository.save(user);
                log.warn("Bootstrap admin {} repaired (role/active/verified enforced)", email);
            } else {
                log.info("Bootstrap admin {} already exists, nothing to do", email);
            }
        }, () -> {
            User admin = User.builder()
                    .name(adminName)
                    .email(email)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .locale("en")
                    .emailVerifiedAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.warn("Bootstrap admin {} created from ADMIN_EMAIL", email);
        });
    }
}
