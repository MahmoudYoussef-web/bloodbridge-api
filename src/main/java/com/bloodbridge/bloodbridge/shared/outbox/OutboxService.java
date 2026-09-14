package com.bloodbridge.bloodbridge.shared.outbox;

import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import com.bloodbridge.bloodbridge.shared.events.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final Map<String, Class<? extends DomainEvent>> eventTypeRegistry;

    @Transactional
    public void saveEvent(DomainEvent event, String aggregateType, Long aggregateId) {
        try {
            // The relay re-publishes events back through the Spring context, which
            // re-triggers the AFTER_COMMIT listeners. Guard by event id so a relayed
            // event never creates a duplicate outbox row (and an endless chain).
            if (outboxRepository.existsById(event.getEventId())) {
                log.debug("Outbox event {} already stored, skipping duplicate", event.getEventId());
                return;
            }
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(event.getEventId())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(event.getEventType())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outboxEvent);
            log.debug("Saved outbox event: {} for {} #{}", event.getEventType(), aggregateType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to save outbox event: {}", e.getMessage());
        }
    }

    public List<OutboxEvent> findPendingEvents(int maxAgeMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        return outboxRepository.findPendingEvents(OutboxStatus.PENDING, threshold);
    }

    @Transactional
    public void markProcessed(String eventId) {
        outboxRepository.markAsProcessed(eventId, OutboxStatus.COMPLETED, LocalDateTime.now());
    }

    @Transactional
    public void markFailed(String eventId, String error) {
        outboxRepository.markAsFailed(eventId, error);
    }

    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pending = findPendingEvents(1);
        for (OutboxEvent event : pending) {
            try {
                DomainEvent domainEvent = deserialize(event);
                if (domainEvent == null) {
                    log.warn("Skipping outbox event {} - no registered type for '{}'", event.getId(), event.getEventType());
                    continue;
                }
                domainEventPublisher.publish(domainEvent);
                markProcessed(event.getId());
                log.info("Processed outbox event: {} (id={})", event.getEventType(), event.getId());
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
                markFailed(event.getId(), e.getMessage());
            }
        }
    }

    private DomainEvent deserialize(OutboxEvent outboxEvent) {
        Class<? extends DomainEvent> type = eventTypeRegistry.get(outboxEvent.getEventType());
        if (type == null) {
            throw new IllegalStateException("No registered type for '" + outboxEvent.getEventType() + "'");
        }
        String payload = outboxEvent.getPayload();
        try {
            // Tolerate payloads that were stored as a JSON-encoded string
            // (double-encoded) instead of a raw JSON object.
            String trimmed = payload != null ? payload.strip() : "";
            if (trimmed.startsWith("\"")) {
                payload = objectMapper.readValue(payload, String.class);
            }
            return objectMapper.readValue(payload, type);
        } catch (Exception e) {
            log.error("Failed to deserialize outbox event {} as {}: {}", outboxEvent.getId(), type.getSimpleName(), e.getMessage());
            throw new IllegalStateException("Failed to deserialize outbox event " + outboxEvent.getId(), e);
        }
    }
}
