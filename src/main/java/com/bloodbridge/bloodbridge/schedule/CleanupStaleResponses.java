package com.bloodbridge.bloodbridge.schedule;

import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupStaleResponses {

    private final RequestResponseRepository requestResponseRepository;
    private final BloodRequestRepository bloodRequestRepository;

    @Value("${bloodbridge.cleanup.stale-critical-hours:8}")
    private int staleCriticalHours;

    @Value("${bloodbridge.cleanup.stale-normal-hours:48}")
    private int staleNormalHours;

    @Transactional
    public int execute() {
        LocalDateTime criticalThreshold = LocalDateTime.now().minusHours(staleCriticalHours);
        LocalDateTime normalThreshold = LocalDateTime.now().minusHours(staleNormalHours);

        List<RequestResponse> staleResponses = requestResponseRepository
                .findStalePendingResponses(RequestResponseStatus.PENDING, normalThreshold);

        int count = 0;

        for (RequestResponse response : staleResponses) {
            boolean isCritical = bloodRequestRepository.findById(response.getBloodRequestId())
                    .map(r -> r.getUrgencyLevel() == UrgencyLevel.CRITICAL)
                    .orElse(false);

            LocalDateTime effectiveThreshold = isCritical ? criticalThreshold : normalThreshold;

            if (response.getRespondedAt() != null && response.getRespondedAt().isBefore(effectiveThreshold)) {
                response.setStatus(RequestResponseStatus.UNREACHABLE);
                requestResponseRepository.save(response);
                count++;
            }
        }

        if (count > 0) {
            log.info("CleanupStaleResponses: marked {} responses as UNREACHABLE", count);
        }

        return count;
    }
}