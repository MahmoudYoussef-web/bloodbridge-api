package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.entity.User;

import java.time.LocalDateTime;

/** Safe admin projection of {@link User}: no password, tokens, or relations. */
public record AdminUserView(
        Long id,
        String name,
        String email,
        String phone,
        Integer role,
        Boolean isActive,
        String locale,
        Boolean emailVerified,
        LocalDateTime createdAt
) {
    public static AdminUserView of(User u) {
        return new AdminUserView(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole() != null ? u.getRole().getValue() : null,
                u.getIsActive(),
                u.getLocale(),
                u.isEmailVerified(),
                u.getCreatedAt()
        );
    }
}
