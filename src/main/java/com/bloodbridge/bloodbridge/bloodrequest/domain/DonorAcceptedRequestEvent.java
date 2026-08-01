package com.bloodbridge.bloodbridge.bloodrequest.domain;

import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DonorAcceptedRequestEvent extends DomainEvent {
    private final Long responseId;
    private final Long donorId;
    private final Long bloodRequestId;
    private final Double distance;

    @JsonCreator
    public DonorAcceptedRequestEvent(@JsonProperty("responseId") Long responseId,
                                      @JsonProperty("donorId") Long donorId,
                                      @JsonProperty("bloodRequestId") Long bloodRequestId,
                                      @JsonProperty("distance") Double distance) {
        this.responseId = responseId;
        this.donorId = donorId;
        this.bloodRequestId = bloodRequestId;
        this.distance = distance;
    }

    @Override
    public String getEventType() {
        return "donor.accepted_request";
    }
}
