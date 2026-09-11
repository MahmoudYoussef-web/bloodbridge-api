package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorHealthProfile;
import com.bloodbridge.bloodbridge.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Flat admin projection of a donor: no lazy collections, no back-references. */
public record AdminDonorView(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String nationalId,
        Integer gender,
        LocalDate birthDate,
        Long governorateId,
        Integer points,
        Integer level,
        Integer bloodType,
        Integer totalDonations,
        Boolean isEligible,
        LocalDateTime createdAt
) {
    public static AdminDonorView of(Donor donor) {
        User user = donor.getUser();
        DonorHealthProfile hp = donor.getHealthProfile();
        return new AdminDonorView(
                donor.getId(),
                donor.getUserId(),
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                donor.getNationalId(),
                donor.getGender() != null ? donor.getGender().getValue() : null,
                donor.getBirthDate(),
                donor.getGovernorateId(),
                donor.getPoints(),
                donor.getLevel(),
                hp != null && hp.getBloodType() != null ? hp.getBloodType().getValue() : null,
                hp != null ? hp.getTotalDonations() : null,
                hp != null ? hp.getIsEligible() : null,
                donor.getCreatedAt()
        );
    }
}
