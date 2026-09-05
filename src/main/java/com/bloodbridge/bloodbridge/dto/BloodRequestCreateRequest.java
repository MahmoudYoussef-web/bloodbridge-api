package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodRequestCreateRequest {
    @NotNull(message = "Blood type is required")
    private BloodType bloodType;

    @NotNull(message = "Units needed is required")
    @Min(value = 1, message = "At least 1 unit is required")
    @Max(value = 20, message = "Maximum 20 units per request")
    private Integer unitsNeeded;

    @Builder.Default
    private UrgencyLevel urgencyLevel = UrgencyLevel.NORMAL;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String additionalNotes;

    @Min(value = 1, message = "Search radius must be at least 1km")
    @Max(value = 100, message = "Search radius must be at most 100km")
    @Builder.Default
    private Integer searchRadiusKm = 10;

    private Double lat;
    private Double lng;

    @Size(max = 1000, message = "Address must be at most 1000 characters")
    private String locationAddress;
}
