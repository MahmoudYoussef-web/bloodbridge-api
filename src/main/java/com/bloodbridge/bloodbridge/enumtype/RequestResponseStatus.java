package com.bloodbridge.bloodbridge.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequestResponseStatus {
    PENDING(0, "Agree", "warning"),
    ACCEPTED(1, "Attended", "success"),
    DECLINED(2, "Medical Exclusion", "danger"),
    COMPLETED(3, "Donated Successfully", "success"),
    IGNORED(4, "Apologized", "danger"),
    NO_SHOW(5, "Did Not Attend", "danger"),
    UNREACHABLE(6, "Unreachable", "gray"),
    NOT_NEEDED(7, "Not Needed", "gray");

    private final int value;
    private final String label;
    private final String color;

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static RequestResponseStatus fromJson(Object raw) {
        if (raw instanceof Integer i) return fromValue(i);
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return fromValue(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return RequestResponseStatus.valueOf(s);
            }
        }
        throw new IllegalArgumentException("Unsupported RequestResponseStatus token: " + raw);
    }

    public static RequestResponseStatus fromValue(int value) {
        for (RequestResponseStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RequestResponseStatus value: " + value);
    }
    
    public boolean isActiveResponse() {
        return this == PENDING || this == ACCEPTED || this == COMPLETED;
    }
    
    public boolean isFinal() {
        return this == COMPLETED || this == DECLINED || this == IGNORED || this == NO_SHOW || this == UNREACHABLE || this == NOT_NEEDED;
    }
}