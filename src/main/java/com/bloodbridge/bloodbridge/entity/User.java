package com.bloodbridge.bloodbridge.entity;

import com.bloodbridge.bloodbridge.enumtype.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.UserRoleConverter.class)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "remember_token", length = 100)
    private String rememberToken;

    @Builder.Default
    @Column(length = 5)
    private String locale = "en";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Donor donor;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Organization organization;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
    
    public String getDashboardUrl() {
        return switch (role) {
            case ADMIN -> "/admin";
            case DONOR -> "/donor";
            case ORGANIZATION -> organization != null ? "/org/" + organization.getSlug() : "/";
        };
    }
    
    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    public boolean canAccessPanel(String panelId) {
        if (!isActive) {
            return false;
        }
        return switch (panelId) {
            case "admin" -> role == UserRole.ADMIN;
            case "donor" -> role == UserRole.DONOR;
            case "organization" -> role == UserRole.ORGANIZATION;
            default -> false;
        };
    }
}