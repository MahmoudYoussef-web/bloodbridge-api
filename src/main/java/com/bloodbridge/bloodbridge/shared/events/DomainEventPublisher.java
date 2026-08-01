package com.bloodbridge.bloodbridge.shared.events;

import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {} (id={})", event.getEventType(), event.getEventId());
        applicationEventPublisher.publishEvent(event);
    }
}
