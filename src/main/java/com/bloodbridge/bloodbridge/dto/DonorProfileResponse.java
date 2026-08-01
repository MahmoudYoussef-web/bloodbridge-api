package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.Gender;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DonorProfileResponse(
    Long id,
    Long userId,
    String name,
    String email,
    String phone,
    String nationalId,
    Gender gender,
    LocalDate birthDate,
    Long governorateId,
    String autoLocationAddress,
    Double lat,
    Double lng,
    Integer points,
    Integer level,
    Integer weight,
    Integer height,
    BloodType bloodType,
    BloodType verifiedBloodType,
    Boolean chronicDisease,
    Boolean recentDonation,
    Boolean infection,
    Boolean isEligible,
    Boolean hasRecentSurgery,
    LocalDate surgeryDate,
    LocalDate nextEligibleDate,
    LocalDate lastDonationDate,
    Integer totalDonations,
    LocalDateTime createdAt
) {}
