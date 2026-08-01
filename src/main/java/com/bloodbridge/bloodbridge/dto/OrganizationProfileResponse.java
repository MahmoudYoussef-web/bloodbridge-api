package com.bloodbridge.bloodbridge.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OrganizationProfileResponse(
    Long id,
    Long userId,
    String orgName,
    String slug,
    String description,
    String licenseNumber,
    String licenseDocumentPath,
    String responsiblePersonName,
    String responsiblePersonPosition,
    String responsiblePersonEmail,
    String contactEmail,
    String contactPhone,
    String streetAddress,
    String autoLocationAddress,
    Double lat,
    Double lng,
    String openingTime,
    String closingTime,
    List<Integer> workingDays,
    Integer dailyCapacity,
    Long governorateId,
    Integer approvalStatus,
    String rejectionReason,
    String createdAt,
    String updatedAt
) {}
