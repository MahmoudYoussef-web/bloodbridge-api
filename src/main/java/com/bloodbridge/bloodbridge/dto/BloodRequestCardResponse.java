package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import lombok.Builder;

@Builder
public record BloodRequestCardResponse(
    Long id,
    Long organizationId,
    String organizationName,
    BloodType bloodType,
    Integer unitsNeeded,
    UrgencyLevel urgencyLevel,
    String additionalNotes,
    Integer searchRadiusKm,
    Double lat,
    Double lng,
    String locationAddress,
    BloodRequestStatus status,
    String broadcastedAt,
    String fulfilledAt,
    Integer actualSearchRadiusKm,
    Double distance,
    RequestResponseStatus myStatus,
    Integer responsesCount,
    Integer donorsAccepted,
    Integer donorsCompleted,
    String createdAt,
    String updatedAt
) {}
