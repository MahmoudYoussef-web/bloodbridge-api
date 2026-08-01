package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.AuthRequest;
import com.bloodbridge.bloodbridge.dto.AuthResponse;
import com.bloodbridge.bloodbridge.dto.RegisterRequest;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.jwt.JwtService;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager, donorRepository, organizationRepository);
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
}
