package com.bloodbridge.bloodbridge.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrganizationStatus {
    PENDING(0, "Pending", "warning"),
    APPROVED(1, "Approved", "success"),
    REJECTED(2, "Rejected", "danger"),
    SUSPENDED(3, "Suspended", "gray");

    private final int value;
    private final String label;
    private final String color;

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static OrganizationStatus fromJson(Object raw) {
        if (raw instanceof Integer i) return fromValue(i);
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return fromValue(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OrganizationStatus.valueOf(s);
            }
        }
        throw new IllegalArgumentException("Unsupported OrganizationStatus token: " + raw);
    }

    public static OrganizationStatus fromValue(int value) {
        for (OrganizationStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown OrganizationStatus value: " + value);
    }
    
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    public boolean isPendingOrRejected() {
        return this == PENDING || this == REJECTED;
    }
}