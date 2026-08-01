package com.bloodbridge.bloodbridge.job;

import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.NotificationType;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.notification.ResponseNotNeededNotification;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import com.bloodbridge.bloodbridge.service.NotificationService;
import com.bloodbridge.bloodbridge.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelExcessResponsesJob {

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final QRCodeService qrCodeService;
    private final NotificationService notificationService;

    @Async("jobExecutor")
    @Transactional
    public void execute(Long bloodRequestId) {
        BloodRequest bloodRequest = bloodRequestRepository.findByIdNotDeleted(bloodRequestId).orElse(null);
        if (bloodRequest == null) return;

        BloodRequestStatus status = bloodRequest.getStatus();
        if (status != BloodRequestStatus.FULFILLED && status != BloodRequestStatus.EXPIRED) return;

        List<RequestResponse> pendingResponses = requestResponseRepository
                .findByBloodRequestIdAndStatus(bloodRequestId, RequestResponseStatus.PENDING);

        if (pendingResponses.isEmpty()) return;

        String reason = status == BloodRequestStatus.EXPIRED ? "expired" : "fulfilled";
        log.info("Canceling {} pending responses for BloodRequest #{} (reason: {})",
                pendingResponses.size(), bloodRequestId, reason);

        for (RequestResponse response : pendingResponses) {
            try {
                int affected = requestResponseRepository.updateStatusWhere(
                        response.getId(), RequestResponseStatus.PENDING, RequestResponseStatus.NOT_NEEDED);
                if (affected == 0) continue;

                response.setStatus(RequestResponseStatus.NOT_NEEDED);
                response.setVerificationQrCode(null);
                response.setQrCodeExpiresAt(null);
                requestResponseRepository.save(response);

                if (response.getDonor() != null && response.getDonor().getUser() != null) {
                    ResponseNotNeededNotification notification = new ResponseNotNeededNotification(
                            response.getId(),
                            "Thank you for your noble initiative 🤍",
                            "The required blood units have been secured thanks to other donors. "
                                    + "We apologize for canceling your appointment, and we hope you will join us in saving another life soon."
                    );
                    notificationService.send(response.getDonor().getUser(), notification,
                            NotificationType.RESPONSE_NOT_NEEDED);
                }
            } catch (Exception e) {
                log.error("Failed to cancel excess response #{}: {}", response.getId(), e.getMessage());
            }
        }
    }
}
