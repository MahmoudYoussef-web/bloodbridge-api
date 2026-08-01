package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.BloodRequestLiteView;
import com.bloodbridge.bloodbridge.dto.DonorLiteView;
import com.bloodbridge.bloodbridge.dto.RequestResponseView;
import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.Organization;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds {@link RequestResponseView} instances from raw entities without
 * triggering N+1 lazy-load reads.
 *
 * Strategy: collect every distinct {@code bloodRequestId} and {@code donorId}
 * referenced by the input response rows, batch-fetch the related entities
 * via {@code findAllById}, batch-fetch the related Organizations and Users
 * for the org-name + user-name leaves the UI reads, then assemble.
 */
@Service
@RequiredArgsConstructor
public class RequestResponseViewAssembler {

    private final BloodRequestRepository bloodRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final DonorRepository donorRepository;
    private final UserRepository userRepository;

    public List<RequestResponseView> assemble(List<RequestResponse> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        Set<Long> bloodRequestIds = new HashSet<>();
        Set<Long> donorIds = new HashSet<>();
        for (RequestResponse rr : rows) {
            if (rr.getBloodRequestId() != null) bloodRequestIds.add(rr.getBloodRequestId());
            if (rr.getDonorId() != null) donorIds.add(rr.getDonorId());
        }

        Map<Long, BloodRequest> brById = bloodRequestRepository.findAllById(bloodRequestIds).stream()
                .collect(Collectors.toMap(BloodRequest::getId, b -> b, (a, b) -> a));

        Set<Long> orgIds = brById.values().stream()
                .map(BloodRequest::getOrganizationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> orgNameById = new HashMap<>();
        if (!orgIds.isEmpty()) {
            organizationRepository.findAllById(orgIds).forEach(o -> orgNameById.put(o.getId(), o.getOrgName()));
        }

        Map<Long, Donor> donorById = donorRepository.findAllById(donorIds).stream()
                .collect(Collectors.toMap(Donor::getId, d -> d, (a, b) -> a));

        Set<Long> userIds = donorById.values().stream()
                .map(Donor::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userNameById = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userNameById.put(u.getId(), u.getName()));
        }

        return rows.stream()
                .map(rr -> RequestResponseView.of(
                        rr,
                        buildBloodRequestLite(rr, brById, orgNameById),
                        buildDonorLite(rr, donorById, userNameById)))
                .collect(Collectors.toList());
    }

    private BloodRequestLiteView buildBloodRequestLite(
            RequestResponse rr,
            Map<Long, BloodRequest> brById,
            Map<Long, String> orgNameById) {
        BloodRequest br = brById.get(rr.getBloodRequestId());
        if (br == null) return null;
        String orgName = orgNameById.get(br.getOrganizationId());
        return BloodRequestLiteView.of(br, orgName);
    }

    private DonorLiteView buildDonorLite(
            RequestResponse rr,
            Map<Long, Donor> donorById,
            Map<Long, String> userNameById) {
        Donor donor = donorById.get(rr.getDonorId());
        if (donor == null) return null;
        String userName = userNameById.get(donor.getUserId());
        return DonorLiteView.of(userName);
    }
}
