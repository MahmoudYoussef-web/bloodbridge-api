package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.AuthRequest;
import com.bloodbridge.bloodbridge.dto.AuthResponse;
import com.bloodbridge.bloodbridge.dto.ChangePasswordRequest;
import com.bloodbridge.bloodbridge.dto.ForgotPasswordRequest;
import com.bloodbridge.bloodbridge.dto.ForgotPasswordResponse;
import com.bloodbridge.bloodbridge.dto.MessageResponse;
import com.bloodbridge.bloodbridge.dto.RegisterRequest;
import com.bloodbridge.bloodbridge.dto.ResendVerificationRequest;
import com.bloodbridge.bloodbridge.dto.ResendVerificationResponse;
import com.bloodbridge.bloodbridge.dto.ResetPasswordRequest;
import com.bloodbridge.bloodbridge.dto.VerifyEmailRequest;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.Organization;
import com.bloodbridge.bloodbridge.entity.PasswordResetToken;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.jwt.JwtService;
import com.bloodbridge.bloodbridge.shared.domain.RedisRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import com.bloodbridge.bloodbridge.repository.PasswordResetTokenRepository;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final long RESET_TOKEN_TTL_MINUTES = 30;
    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final DonorRepository donorRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Environment environment;
    private final ObjectProvider<RedisRateLimiter> redisRateLimiterProvider;

    private RedisRateLimiter redisRateLimiterOrNull() {
        return redisRateLimiterProvider.getIfAvailable();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new BusinessException("Password confirmation does not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT);
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role. Must be DONOR or ORGANIZATION");
        }

        if (role == UserRole.ADMIN) {
            throw new BusinessException("Cannot self-register as ADMIN", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                .locale("en")
                .verificationToken(generateToken())
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS))
                .build();

        user = userRepository.save(user);

        if (role == UserRole.DONOR) {
            donorRepository.save(Donor.builder()
                    .userId(user.getId())
                    .build());
        } else if (role == UserRole.ORGANIZATION) {
            organizationRepository.save(Organization.builder()
                    .userId(user.getId())
                    .orgName(request.getName())
                    .slug(request.getName().toLowerCase().replaceAll("\\s+", "-"))
                    .approvalStatus(OrganizationStatus.APPROVED)
                    .build());
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .dashboardUrl(user.getDashboardUrl())
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .dashboardUrl(user.getDashboardUrl())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        RedisRateLimiter limiter = redisRateLimiterOrNull();
        if (limiter == null) {
            log.warn("Redis is disabled: refresh-token rotation runs without revocation tracking (reuse cannot be detected)");
        }
        if (limiter != null && limiter.isTokenBlacklisted(refreshToken)) {
            throw new BusinessException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BusinessException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtService.extractUsername(refreshToken);
        Long userId = jwtService.extractUserId(refreshToken);
        String role = jwtService.extractRole(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        String newToken = jwtService.generateToken(userId, email, role);
        String newRefreshToken = jwtService.generateRefreshToken(userId, email, role);

        // Rotation: revoke the used refresh token to detect reuse.
        if (limiter != null) {
            long ttl = jwtService.getRemainingSeconds(refreshToken);
            if (ttl > 0) limiter.blacklistToken(refreshToken, ttl);
        }

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .dashboardUrl(user.getDashboardUrl())
                .build();
    }

    /**
     * Creates a password-reset token for the given email. Always returns a
     * success-shaped response to avoid leaking which addresses are registered;
     * under the dev "h2" profile the generated token is returned in the payload
     * so the flow can be exercised without a mail server.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);

        String devResetToken = null;
        if (user != null) {
            String token = generateToken();
            passwordResetTokenRepository.deleteById(email);
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .email(email)
                    .token(token)
                    .build());

            if (environment.acceptsProfiles(Profiles.of("h2"))) {
                devResetToken = token;
            }
        }

        return new ForgotPasswordResponse(
                "If that email is registered, a password reset link has been sent.",
                devResetToken);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new BusinessException("Password confirmation does not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        if (resetToken.getCreatedAt().plusMinutes(RESET_TOKEN_TTL_MINUTES).isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.deleteById(resetToken.getEmail());
            throw new BusinessException("Invalid or expired reset token");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(resetToken.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.deleteById(resetToken.getEmail());

        return new MessageResponse("Password has been reset successfully");
    }

    @Transactional
    public MessageResponse changePassword(User user, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getNewPasswordConfirmation())) {
            throw new BusinessException("Password confirmation does not match");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new MessageResponse("Password changed successfully");
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByVerificationToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired verification token"));

        if (user.getVerificationTokenExpiresAt() == null
                || user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invalid or expired verification token");
        }

        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        return new MessageResponse("Email verified successfully");
    }

    /**
     * Regenerates a verification token for the given email. Under the dev "h2"
     * profile the token is returned so local verification can complete without
     * a mail server (mirrors DevVerifyProbe).
     */
    @Transactional
    public ResendVerificationResponse resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found: " + request.getEmail()));

        String token = generateToken();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS));
        userRepository.save(user);

        String devVerificationToken = environment.acceptsProfiles(Profiles.of("h2")) ? token : null;
        return new ResendVerificationResponse("Verification email sent", devVerificationToken);
    }

    public MessageResponse logout(String token) {
        RedisRateLimiter limiter = redisRateLimiterOrNull();
        if (limiter != null && token != null && !token.isBlank()) {
            long ttl = jwtService.getRemainingSeconds(token);
            if (ttl <= 0) ttl = 86400;
            limiter.blacklistToken(token, ttl);
        }
        return new MessageResponse("Logged out successfully");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}