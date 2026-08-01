package com.bloodbridge.bloodbridge.bloodrequest.domain;

import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class BloodRequestBroadcastedEvent extends DomainEvent {
    private final Long bloodRequestId;
    private final Long organizationId;
    private final int donorCount;
    private final List<Long> donorIds;
    private final int searchRadiusKm;

    @JsonCreator
    public BloodRequestBroadcastedEvent(@JsonProperty("bloodRequestId") Long bloodRequestId,
                                         @JsonProperty("organizationId") Long organizationId,
                                         @JsonProperty("donorCount") int donorCount,
                                         @JsonProperty("donorIds") List<Long> donorIds,
                                         @JsonProperty("searchRadiusKm") int searchRadiusKm) {
        this.bloodRequestId = bloodRequestId;
        this.organizationId = organizationId;
        this.donorCount = donorCount;
        this.donorIds = donorIds;
        this.searchRadiusKm = searchRadiusKm;
    }

    @Override
    public String getEventType() {
        return "blood_request.broadcasted";
    }
}
