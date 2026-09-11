package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonorProfileUpdateRequest {
    private String name;
    private String phone;
    @Pattern(regexp = "^\\d{14}$", message = "National ID must be exactly 14 digits")
    private String nationalId;
    private Gender gender;
    @Past(message = "Birth date must be in the past")
    private java.time.LocalDate birthDate;
    private Integer weight;
    private Integer height;
    private BloodType bloodType;
    private Boolean chronicDisease;
    private Boolean infection;
    private Boolean recentDonation;
    private java.time.LocalDate lastDonationDate;
    private Boolean hasRecentSurgery;
    private java.time.LocalDate surgeryDate;

    private Double lat;
    private Double lng;
    private Long governorateId;
    private String autoLocationAddress;
}
