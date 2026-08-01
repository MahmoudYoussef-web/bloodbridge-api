package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonorProfileUpdateRequest {
    private String name;
    private String phone;
    private Integer weight;
    private Integer height;
    private BloodType bloodType;
    private Boolean chronicDisease;
    private Boolean infection;
    private Boolean hasRecentSurgery;
    private java.time.LocalDate surgeryDate;
}
