package com.bloodbridge.bloodbridge.shared.outbox;

import com.bloodbridge.bloodbridge.bloodrequest.domain.BloodRequestBroadcastedEvent;
import com.bloodbridge.bloodbridge.shared.domain.DomainEvent;
import com.bloodbridge.bloodbridge.shared.events.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock private OutboxRepository outboxRepository;
    @Mock private DomainEventPublisher domainEventPublisher;

    private OutboxService outboxService;
    private ObjectMapper objectMapper;
    private Map<String, Class<? extends DomainEvent>> eventTypeRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        eventTypeRegistry = new HashMap<>();
        eventTypeRegistry.put("blood_request.broadcasted", BloodRequestBroadcastedEvent.class);
        outboxService = new OutboxService(outboxRepository, objectMapper, domainEventPublisher, eventTypeRegistry);
    }

    @Test
    void shouldSaveOutboxEvent() {
        BloodRequestBroadcastedEvent event = new BloodRequestBroadcastedEvent(
                1L, 1L, 5, List.of(1L, 2L), 10);

        outboxService.saveEvent(event, "BloodRequest", 1L);

        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldHandleSerializationException() {
        DomainEvent badEvent = new DomainEvent() {
            @Override public String getEventType() { return "bad.event"; }
            @SuppressWarnings("unused")
            public Object getSelf() { return this; }
        };

        outboxService.saveEvent(badEvent, "Test", 1L);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void shouldFindPendingEvents() {
        when(outboxRepository.findPendingEvents(any(OutboxStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<OutboxEvent> events = outboxService.findPendingEvents(30);

        assertThat(events).isEmpty();
    }

    @Test
    void shouldMarkEventAsProcessed() {
        outboxService.markProcessed("event-1");

        verify(outboxRepository).markAsProcessed(eq("event-1"), eq(OutboxStatus.COMPLETED), any(LocalDateTime.class));
    }

    @Test
    void shouldMarkEventAsFailed() {
        outboxService.markFailed("event-1", "Something went wrong");

        verify(outboxRepository).markAsFailed("event-1", "Something went wrong");
    }

    @Test
    void shouldProcessPendingEvents() {
        OutboxEvent pendingEvent = OutboxEvent.builder()
                .id("event-1")
                .eventType("blood_request.broadcasted")
                .payload("{\"bloodRequestId\":1,\"organizationId\":1,\"donorCount\":1,\"donorIds\":[1],\"searchRadiusKm\":10}")
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(outboxRepository.findPendingEvents(any(OutboxStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingEvent));

        outboxService.processPendingEvents();

        verify(domainEventPublisher).publish(any(BloodRequestBroadcastedEvent.class));
        verify(outboxRepository).markAsProcessed(eq("event-1"), eq(OutboxStatus.COMPLETED), any(LocalDateTime.class));
    }
}
