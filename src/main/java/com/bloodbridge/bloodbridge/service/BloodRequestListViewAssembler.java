package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.BloodRequestListView;
import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.repository.BloodRequestAggregate;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds {@link BloodRequestListView} instances for a list of
 * {@link BloodRequest} entities, fetching the per-request aggregate
 * counts in a single GROUP-BY query.
 *
 * No N+1: one query to load the count aggregates for all request ids,
 * then one pass to assemble.
 *
 * Status semantics (locked by user 2026-07-30):
 *   - donorsAccepted = count where status IN (ACCEPTED, COMPLETED).  PENDING
 *     is excluded by design — a PENDING donor has not yet responded and
 *     should not count as "accepted" in the org UI.
 */
@Service
@RequiredArgsConstructor
public class BloodRequestListViewAssembler {

    private final RequestResponseRepository requestResponseRepository;

    public List<BloodRequestListView> assemble(List<BloodRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();

        List<Long> requestIds = requests.stream()
                .map(BloodRequest::getId)
                .collect(Collectors.toList());

        List<RequestResponseStatus> acceptedStatuses = List.of(
                RequestResponseStatus.ACCEPTED,
                RequestResponseStatus.COMPLETED);

        Map<Long, BloodRequestAggregate> aggById = new HashMap<>();
        List<BloodRequestAggregate> rows = requestResponseRepository
                .aggregateBloodRequestCountsByRequestIds(requestIds, acceptedStatuses, RequestResponseStatus.COMPLETED);
        if (rows != null) {
            for (BloodRequestAggregate a : rows) {
                aggById.put(a.getBloodRequestId(), a);
            }
        }

        return requests.stream()
                .map(br -> {
                    BloodRequestAggregate agg = aggById.get(br.getId());
                    if (agg == null) {
                        return BloodRequestListView.of(br, 0L, 0L, 0L);
                    }
                    return BloodRequestListView.of(
                            br,
                            agg.getResponsesCount() == null ? 0L : agg.getResponsesCount(),
                            agg.getDonorsAccepted() == null ? 0L : agg.getDonorsAccepted(),
                            agg.getDonorsCompleted() == null ? 0L : agg.getDonorsCompleted());
                })
                .collect(Collectors.toList());
    }
}
