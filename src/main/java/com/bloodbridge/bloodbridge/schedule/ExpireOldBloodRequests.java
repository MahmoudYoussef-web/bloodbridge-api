package com.bloodbridge.bloodbridge.schedule;

import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.job.CancelExcessResponsesJob;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
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
public class ExpireOldBloodRequests {

    private final BloodRequestRepository bloodRequestRepository;
    private final CancelExcessResponsesJob cancelExcessResponsesJob;

    @Value("${bloodbridge.cleanup.expire-hours:48}")
    private int expireHours;

    @Transactional
    public int execute() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(expireHours);

        List<BloodRequest> expiredRequests = bloodRequestRepository
                .findByStatusInAndCreatedAtBefore(
                        List.of(BloodRequestStatus.PENDING, BloodRequestStatus.BROADCASTED),
                        threshold);

        int count = 0;

        for (BloodRequest request : expiredRequests) {
            request.setStatus(BloodRequestStatus.EXPIRED);
            bloodRequestRepository.save(request);

            cancelExcessResponsesJob.execute(request.getId());

            log.info("Expired blood request {} (urgency={})", request.getId(), request.getUrgencyLevel());
            count++;
        }

        if (count > 0) {
            log.info("ExpireOldBloodRequests: expired {} requests", count);
        }

        return count;
    }
}