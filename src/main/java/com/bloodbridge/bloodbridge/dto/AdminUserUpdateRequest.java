package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.UserRole;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Whitelist for admin user edits. Only these fields may be mass-assigned;
 * role changes to/from ADMIN are rejected in the controller.
 */
@Getter
@Setter
public class AdminUserUpdateRequest {
    private UserRole role;
    private Boolean isActive;
    @Size(max = 5, message = "Locale is too long")
    private String locale;
}
