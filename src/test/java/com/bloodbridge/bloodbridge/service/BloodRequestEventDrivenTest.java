package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.bloodrequest.domain.BloodRequestBroadcastedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonationCompletedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonorAcceptedRequestEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BloodRequestEventDrivenTest {

    @Test
    void shouldCreateBloodRequestBroadcastedEvent() {
        BloodRequestBroadcastedEvent event = new BloodRequestBroadcastedEvent(
                1L, 1L, 5, List.of(1L, 2L, 3L), 15);

        assertThat(event.getEventType()).isEqualTo("blood_request.broadcasted");
        assertThat(event.getBloodRequestId()).isEqualTo(1L);
        assertThat(event.getDonorCount()).isEqualTo(5);
        assertThat(event.getDonorIds()).hasSize(3);
        assertThat(event.getSearchRadiusKm()).isEqualTo(15);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
    }

    @Test
    void shouldCreateDonationCompletedEvent() {
        DonationCompletedEvent event = new DonationCompletedEvent(1L, 2L, 3L, 4L);

        assertThat(event.getEventType()).isEqualTo("donation.completed");
        assertThat(event.getResponseId()).isEqualTo(1L);
        assertThat(event.getDonorId()).isEqualTo(2L);
        assertThat(event.getBloodRequestId()).isEqualTo(3L);
        assertThat(event.getOrganizationId()).isEqualTo(4L);
    }

    @Test
    void shouldCreateDonorAcceptedRequestEvent() {
        DonorAcceptedRequestEvent event = new DonorAcceptedRequestEvent(1L, 2L, 3L, 5.5);

        assertThat(event.getEventType()).isEqualTo("donor.accepted_request");
        assertThat(event.getResponseId()).isEqualTo(1L);
        assertThat(event.getDonorId()).isEqualTo(2L);
        assertThat(event.getDistance()).isEqualTo(5.5);
    }

    @Test
    void shouldHaveUniqueEventIds() {
        BloodRequestBroadcastedEvent event1 = new BloodRequestBroadcastedEvent(1L, 1L, 3, List.of(1L), 10);
        BloodRequestBroadcastedEvent event2 = new BloodRequestBroadcastedEvent(1L, 1L, 3, List.of(1L), 10);

        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void shouldRecordOccurredOn() {
        BloodRequestBroadcastedEvent event = new BloodRequestBroadcastedEvent(1L, 1L, 3, List.of(1L), 10);

        assertThat(event.getOccurredOn()).isNotNull();
    }
}
