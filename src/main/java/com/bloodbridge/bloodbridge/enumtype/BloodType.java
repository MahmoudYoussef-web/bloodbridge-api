package com.bloodbridge.bloodbridge.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BloodType {
    O_POSITIVE(1, "O+", "danger"),
    O_NEGATIVE(2, "O-", "danger"),
    A_POSITIVE(3, "A+", "danger"),
    A_NEGATIVE(4, "A-", "danger"),
    B_POSITIVE(5, "B+", "danger"),
    B_NEGATIVE(6, "B-", "danger"),
    AB_POSITIVE(7, "AB+", "danger"),
    AB_NEGATIVE(8, "AB-", "danger"),
    UNKNOWN(9, "Unknown", "gray");

    private final int value;
    private final String label;
    private final String color;

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static BloodType fromJson(Object raw) {
        if (raw instanceof Integer i) return fromValue(i);
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return fromValue(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return BloodType.valueOf(s);
            }
        }
        throw new IllegalArgumentException("Unsupported BloodType token: " + raw);
    }

    public BloodType[] getCompatibleDonorTypes() {
        return switch (this) {
            case O_POSITIVE -> new BloodType[]{O_POSITIVE, O_NEGATIVE};
            case O_NEGATIVE -> new BloodType[]{O_NEGATIVE};
            case A_POSITIVE -> new BloodType[]{A_POSITIVE, A_NEGATIVE, O_POSITIVE, O_NEGATIVE};
            case A_NEGATIVE -> new BloodType[]{A_NEGATIVE, O_NEGATIVE};
            case B_POSITIVE -> new BloodType[]{B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE};
            case B_NEGATIVE -> new BloodType[]{B_NEGATIVE, O_NEGATIVE};
            case AB_POSITIVE -> new BloodType[]{AB_POSITIVE, AB_NEGATIVE, A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE};
            case AB_NEGATIVE -> new BloodType[]{AB_NEGATIVE, A_NEGATIVE, B_NEGATIVE, O_NEGATIVE};
            case UNKNOWN -> new BloodType[]{};
        };
    }

    public BloodType[] getCompatibleRecipientTypes() {
        if (this == UNKNOWN) {
            return new BloodType[]{};
        }
        return java.util.Arrays.stream(BloodType.values())
            .filter(recipient -> recipient != UNKNOWN 
                && java.util.Arrays.asList(recipient.getCompatibleDonorTypes()).contains(this))
            .toArray(BloodType[]::new);
    }
    
    public static BloodType fromValue(int value) {
        for (BloodType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown BloodType value: " + value);
    }
}