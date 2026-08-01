package com.bloodbridge.bloodbridge.shared.events;

import com.bloodbridge.bloodbridge.bloodrequest.domain.BloodRequestBroadcastedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonationCompletedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonorAcceptedRequestEvent;
import com.bloodbridge.bloodbridge.shared.audit.AuditLogService;
import com.bloodbridge.bloodbridge.shared.monitoring.BloodBridgeMetrics;
import com.bloodbridge.bloodbridge.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventListener {

    private final OutboxService outboxService;
    private final AuditLogService auditLogService;
    private final BloodBridgeMetrics metrics;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBloodRequestBroadcasted(BloodRequestBroadcastedEvent event) {
        log.info("Handling BloodRequestBroadcastedEvent: requestId={}, donors={}",
                event.getBloodRequestId(), event.getDonorCount());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDonationCompleted(DonationCompletedEvent event) {
        log.info("Handling DonationCompletedEvent: responseId={}, donorId={}",
                event.getResponseId(), event.getDonorId());
        metrics.incrementDonationComplete();
        outboxService.saveEvent(event, "RequestResponse", event.getResponseId());
        auditLogService.logSimple("RequestResponse", event.getResponseId(),
                "COMPLETED", event.getDonorId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDonorAcceptedRequest(DonorAcceptedRequestEvent event) {
        log.info("Handling DonorAcceptedRequestEvent: donorId={}, requestId={}",
                event.getDonorId(), event.getBloodRequestId());
        auditLogService.logSimple("RequestResponse", event.getResponseId(),
                "ACCEPTED", event.getDonorId());
    }
}
