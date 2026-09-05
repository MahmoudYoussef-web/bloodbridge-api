package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.Gender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonorProfileUpdateRequest {
    private String name;
    private String phone;
    private String nationalId;
    private Gender gender;
    private java.time.LocalDate birthDate;
    private Integer weight;
    private Integer height;
    private BloodType bloodType;
    private Boolean chronicDisease;
    private Boolean infection;
    private Boolean hasRecentSurgery;
    private java.time.LocalDate surgeryDate;

    private Double lat;
    private Double lng;
    private Long governorateId;
    private String autoLocationAddress;
}
