package com.bloodbridge.bloodbridge.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE(1, "Male", "ذكر"),
    FEMALE(2, "Female", "أنثى");

    private final int value;
    private final String labelEn;
    private final String labelAr;

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static Gender fromJson(Object raw) {
        if (raw instanceof Integer i) return fromValue(i);
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return fromValue(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Gender.valueOf(s);
            }
        }
        throw new IllegalArgumentException("Unsupported Gender token: " + raw);
    }

    public static Gender fromValue(int value) {
        for (Gender gender : values()) {
            if (gender.value == value) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown Gender value: " + value);
    }
}