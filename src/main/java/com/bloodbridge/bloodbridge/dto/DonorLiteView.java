package com.bloodbridge.bloodbridge.dto;

/**
 * Wire mirror for the nested {@code Donor} reached from a
 * {@code RequestResponseView}.  Mirrors the frontend TS shape
 * {@code r.donor.user.name} exactly.
 *
 * Excluded (not read by any list-screen consumer):
 *   - donor.id, donor.governorateId, donor.governorate, donor.nationalId,
 *     donor.gender, donor.birthDate, donor.points, donor.level,
 *     donor.healthProfile, donor.createdAt, donor.updatedAt,
 *     user.id, user.email, user.phone, user.role, user.isActive,
 *     user.locale, user.emailVerifiedAt, user.createdAt, user.updatedAt
 */
public record DonorLiteView(UserLiteView user) {

    public record UserLiteView(String name) {}

    public static DonorLiteView of(String userName) {
        return new DonorLiteView(new UserLiteView(userName));
    }
}
