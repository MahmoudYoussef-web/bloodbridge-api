package com.bloodbridge.bloodbridge.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BloodRequestStatus {
    PENDING(0, "Pending", "warning"),
    BROADCASTED(1, "Broadcasted", "info"),
    FULFILLED(3, "Fulfilled", "success"),
    CANCELLED(4, "Cancelled", "danger"),
    EXPIRED(5, "Expired", "danger");

    private final int value;
    private final String label;
    private final String color;

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static BloodRequestStatus fromJson(Object raw) {
        if (raw instanceof Integer i) return fromValue(i);
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return fromValue(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return BloodRequestStatus.valueOf(s);
            }
        }
        throw new IllegalArgumentException("Unsupported BloodRequestStatus token: " + raw);
    }

    public static BloodRequestStatus fromValue(int value) {
        for (BloodRequestStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BloodRequestStatus value: " + value);
    }
    
    public boolean isActive() {
        return this == BROADCASTED;
    }
}