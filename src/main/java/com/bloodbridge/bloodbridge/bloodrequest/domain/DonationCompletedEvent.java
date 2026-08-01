package com.bloodbridge.bloodbridge.bloodrequest.domain;

import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DonationCompletedEvent extends DomainEvent {
    private final Long responseId;
    private final Long donorId;
    private final Long bloodRequestId;
    private final Long organizationId;

    @JsonCreator
    public DonationCompletedEvent(@JsonProperty("responseId") Long responseId,
                                   @JsonProperty("donorId") Long donorId,
                                   @JsonProperty("bloodRequestId") Long bloodRequestId,
                                   @JsonProperty("organizationId") Long organizationId) {
        this.responseId = responseId;
        this.donorId = donorId;
        this.bloodRequestId = bloodRequestId;
        this.organizationId = organizationId;
    }

    @Override
    public String getEventType() {
        return "donation.completed";
    }
}
