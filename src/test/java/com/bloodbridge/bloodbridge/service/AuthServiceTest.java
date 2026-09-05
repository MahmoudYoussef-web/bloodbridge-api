package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.AuthRequest;
import com.bloodbridge.bloodbridge.dto.AuthResponse;
import com.bloodbridge.bloodbridge.dto.ChangePasswordRequest;
import com.bloodbridge.bloodbridge.dto.ForgotPasswordRequest;
import com.bloodbridge.bloodbridge.dto.ForgotPasswordResponse;
import com.bloodbridge.bloodbridge.dto.RegisterRequest;
import com.bloodbridge.bloodbridge.dto.ResetPasswordRequest;
import com.bloodbridge.bloodbridge.dto.VerifyEmailRequest;
import com.bloodbridge.bloodbridge.entity.PasswordResetToken;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.jwt.JwtService;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import com.bloodbridge.bloodbridge.repository.PasswordResetTokenRepository;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private DonorRepository donorRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private Environment environment;
    @Mock private ObjectProvider<com.bloodbridge.bloodbridge.shared.domain.RedisRateLimiter> redisRateLimiterProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager,
                donorRepository, organizationRepository, passwordResetTokenRepository, environment,
                redisRateLimiterProvider);
    }

    @Test
    void shouldRegisterDonor() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Donor")
                .email("donor@test.com")
                .password("password123")
                .passwordConfirmation("password123")
                .phone("123456789")
                .role("DONOR")
                .build();

        when(userRepository.existsByEmail("donor@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(jwtService.generateRefreshToken(any(), any(), any())).thenReturn("refresh");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getEmail()).isEqualTo("donor@test.com");
        assertThat(response.getRole()).isEqualTo("DONOR");
    }

    @Test
    void shouldThrowWhenPasswordMismatch() {
        RegisterRequest request = RegisterRequest.builder()
                .password("pass1")
                .passwordConfirmation("pass2")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password confirmation does not match");
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.com")
                .password("password123")
                .passwordConfirmation("password123")
                .role("DONOR")
                .build();

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void shouldThrowWhenInvalidRole() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@test.com")
                .password("password123")
                .passwordConfirmation("password123")
                .role("INVALID_ROLE")
                .build();

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void shouldAuthenticateUser() {
        AuthRequest request = AuthRequest.builder()
                .email("user@test.com")
                .password("password")
                .build();

        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .name("User")
                .role(UserRole.DONOR)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(jwtService.generateRefreshToken(any(), any(), any())).thenReturn("refresh");

        AuthResponse response = authService.authenticate(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void shouldThrowOnInvalidCredentials() {
        AuthRequest request = AuthRequest.builder()
                .email("user@test.com")
                .password("wrong")
                .build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRefreshToken() {
        String refreshToken = "valid-refresh-token";
        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .name("User")
                .role(UserRole.DONOR)
                .build();

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn("user@test.com");
        when(jwtService.extractUserId(refreshToken)).thenReturn(1L);
        when(jwtService.extractRole(refreshToken)).thenReturn("DONOR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any(), any())).thenReturn("new-token");
        when(jwtService.generateRefreshToken(any(), any(), any())).thenReturn("new-refresh");

        AuthResponse response = authService.refreshToken(refreshToken);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void shouldCreateResetTokenForExistingUser() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(User.builder().email("user@test.com").build()));
        when(environment.acceptsProfiles(Profiles.of("h2"))).thenReturn(false);

        ForgotPasswordResponse response = authService.forgotPassword(
                ForgotPasswordRequest.builder().email("user@test.com").build());

        assertThat(response.message()).contains("reset link");
        assertThat(response.devResetToken()).isNull();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void shouldReturnGenericResponseForUnknownEmail() {
        when(userRepository.findByEmailAndDeletedAtIsNull("ghost@test.com")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = authService.forgotPassword(
                ForgotPasswordRequest.builder().email("ghost@test.com").build());

        assertThat(response.message()).contains("reset link");
        assertThat(response.devResetToken()).isNull();
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        User user = User.builder().email("user@test.com").password("old-encoded").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email("user@test.com")
                .token("reset-token-123")
                .createdAt(LocalDateTime.now())
                .build();

        when(passwordResetTokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(resetToken));
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-encoded");

        var response = authService.resetPassword(ResetPasswordRequest.builder()
                .token("reset-token-123")
                .password("newPassword1")
                .passwordConfirmation("newPassword1")
                .build());

        assertThat(response.message()).contains("reset");
        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(passwordResetTokenRepository).deleteById("user@test.com");
    }

    @Test
    void shouldRejectExpiredResetToken() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email("user@test.com")
                .token("old-token")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(passwordResetTokenRepository.findByToken("old-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordRequest.builder()
                .token("old-token")
                .password("newPassword1")
                .passwordConfirmation("newPassword1")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid or expired reset token");
        verify(passwordResetTokenRepository).deleteById("user@test.com");
    }

    @Test
    void shouldChangePasswordWithCorrectCurrentPassword() {
        User user = User.builder().email("user@test.com").password("current-encoded").build();
        when(passwordEncoder.matches("currentPassword1", "current-encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-encoded");

        var response = authService.changePassword(user, ChangePasswordRequest.builder()
                .currentPassword("currentPassword1")
                .newPassword("newPassword1")
                .newPasswordConfirmation("newPassword1")
                .build());

        assertThat(response.message()).contains("changed");
        assertThat(user.getPassword()).isEqualTo("new-encoded");
    }

    @Test
    void shouldRejectChangePasswordWithWrongCurrent() {
        User user = User.builder().email("user@test.com").password("current-encoded").build();
        when(passwordEncoder.matches("wrong-password", "current-encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(user, ChangePasswordRequest.builder()
                .currentPassword("wrong-password")
                .newPassword("newPassword1")
                .newPasswordConfirmation("newPassword1")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void shouldVerifyEmailWithValidToken() {
        User user = User.builder()
                .email("user@test.com")
                .verificationToken("verify-token-123")
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(userRepository.findByVerificationToken("verify-token-123")).thenReturn(Optional.of(user));

        var response = authService.verifyEmail(VerifyEmailRequest.builder().token("verify-token-123").build());

        assertThat(response.message()).contains("verified");
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getVerificationToken()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectExpiredVerificationToken() {
        User user = User.builder()
                .email("user@test.com")
                .verificationToken("stale-token")
                .verificationTokenExpiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByVerificationToken("stale-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(VerifyEmailRequest.builder().token("stale-token").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid or expired verification token");
    }
}
