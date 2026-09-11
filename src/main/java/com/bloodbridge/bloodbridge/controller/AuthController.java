package com.bloodbridge.bloodbridge.controller;

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
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.service.AuthService;
import com.bloodbridge.bloodbridge.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    private boolean hitLimit(String endpoint, String clientId, int max, int windowSeconds) {
        return !rateLimitService.checkEndpointLimit("auth:" + endpoint, clientId, max, windowSeconds);
    }

    private String clientIp(HttpServletRequest request) {
        return request != null ? request.getRemoteAddr() : "unknown";
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        if (hitLimit("register", clientIp(http), 5, 60)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Too many attempts. Please try again later."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequest request, HttpServletRequest http) {
        if (hitLimit("login", clientIp(http) + ":" + request.getEmail(), 10, 60)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Too many attempts. Please try again later."));
        }
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String authHeader, HttpServletRequest http) {
        if (hitLimit("refresh", clientIp(http), 30, 60)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Too many attempts. Please try again later."));
        }
        String refreshToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null) {
            token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        }
        return ResponseEntity.ok(authService.logout(token));
    }

    @GetMapping("/profile")
    public ResponseEntity<java.util.Map<String, Object>> profile(@AuthenticationPrincipal User user) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("name", user.getName());
        body.put("role", user.getRole() != null ? user.getRole().name() : null);
        body.put("locale", user.getLocale());
        body.put("isActive", user.getIsActive());
        body.put("emailVerified", user.isEmailVerified());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        if (hitLimit("forgot", clientIp(http) + ":" + request.getEmail(), 3, 60)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Too many attempts. Please try again later."));
        }
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        if (hitLimit("reset", clientIp(http), 10, 60)) {
            return ResponseEntity.status(429)
                    .body(new MessageResponse("Too many attempts. Please try again later."));
        }
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(user, request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletRequest http) {
        if (hitLimit("verify", clientIp(http), 10, 60)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Too many attempts. Please try again later."));
        }
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request, HttpServletRequest http) {
        if (!rateLimitService.tryEmailVerification(request.getEmail())
                || hitLimit("resend", clientIp(http), 10, 60)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Too many attempts. Please try again later."));
        }
        return ResponseEntity.ok(authService.resendVerification(request));
    }
}