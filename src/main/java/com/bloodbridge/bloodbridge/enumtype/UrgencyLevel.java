package com.bloodbridge.bloodbridge.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UrgencyLevel {
    NORMAL(1, "Normal", "info"),
    CRITICAL(2, "Critical", "danger");

    private final int value;
    private final String label;
    private final String color;

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static UrgencyLevel fromJson(Object raw) {
        if (raw instanceof Integer i) return fromValue(i);
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return fromValue(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return UrgencyLevel.valueOf(s);
            }
        }
        throw new IllegalArgumentException("Unsupported UrgencyLevel token: " + raw);
    }

    public static UrgencyLevel fromValue(int value) {
        for (UrgencyLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown UrgencyLevel value: " + value);
    }
    
    public boolean isCritical() {
        return this == CRITICAL;
    }
}