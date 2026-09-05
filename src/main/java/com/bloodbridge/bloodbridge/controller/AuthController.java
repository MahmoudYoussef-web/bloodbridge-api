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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
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
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(user, request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ResendVerificationResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }
}