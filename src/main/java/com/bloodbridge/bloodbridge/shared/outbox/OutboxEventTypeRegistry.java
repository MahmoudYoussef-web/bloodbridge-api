package com.bloodbridge.bloodbridge.shared.outbox;

import com.bloodbridge.bloodbridge.bloodrequest.domain.BloodRequestBroadcastedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonationCompletedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonorAcceptedRequestEvent;
import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OutboxEventTypeRegistry {

    @Bean
    public Map<String, Class<? extends DomainEvent>> domainEventTypeRegistry() {
        Map<String, Class<? extends DomainEvent>> registry = new LinkedHashMap<>();
        registry.put("blood_request.broadcasted", BloodRequestBroadcastedEvent.class);
        registry.put("donation.completed", DonationCompletedEvent.class);
        registry.put("donor.accepted_request", DonorAcceptedRequestEvent.class);
        return registry;
    }
}
