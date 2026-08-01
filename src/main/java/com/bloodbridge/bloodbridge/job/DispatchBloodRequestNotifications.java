package com.bloodbridge.bloodbridge.job;

import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorHealthProfile;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.NotificationType;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.notification.BloodRequestMatchNotification;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import com.bloodbridge.bloodbridge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchBloodRequestNotifications {

    private static final int MAX_BATCH_SIZE = 100;

    private final BloodRequestRepository bloodRequestRepository;
    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final NotificationService notificationService;

    @Async("jobExecutor")
    @Transactional
    public void dispatchBatches(Long bloodRequestId, Map<Long, Double> donorData) {
        BloodRequest bloodRequest = bloodRequestRepository.findByIdNotDeleted(bloodRequestId).orElse(null);
        if (bloodRequest == null) {
            log.warn("Blood request {} not found for notification dispatch", bloodRequestId);
            return;
        }

        int chunkCount = 0;
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(donorData.entrySet());
        for (int i = 0; i < entries.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, entries.size());
            Map<Long, Double> chunk = new HashMap<>();
            for (int j = i; j < end; j++) {
                chunk.put(entries.get(j).getKey(), entries.get(j).getValue());
            }
            chunkCount++;
            processChunk(bloodRequest, chunk, chunkCount);
        }

        log.info("Dispatched notifications for request {}: {} donors in {} batches",
                bloodRequestId, donorData.size(), chunkCount);
    }

    private void processChunk(BloodRequest bloodRequest, Map<Long, Double> chunk, int chunkNum) {
        List<Long> userIds = new ArrayList<>(chunk.keySet());
        List<User> users = userRepository.findAllById(userIds);

        List<Long> donorIds = users.stream()
                .map(u -> u.getDonor())
                .filter(Objects::nonNull)
                .map(Donor::getId)
                .collect(Collectors.toList());

        Set<Long> alreadyRespondedIds = new HashSet<>();
        if (!donorIds.isEmpty()) {
            alreadyRespondedIds.addAll(
                    requestResponseRepository.findByBloodRequestIdAndDonorIdIn(
                            bloodRequest.getId(), donorIds
                    ).stream()
                    .map(rr -> rr.getDonorId())
                    .collect(Collectors.toSet())
            );
        }

        int sentCount = 0;
        int skippedCount = 0;

        for (User user : users) {
            if (user.getDonor() == null || user.getDonor().getHealthProfile() == null) {
                skippedCount++;
                continue;
            }

            DonorHealthProfile profile = user.getDonor().getHealthProfile();

            boolean isStillEligible = Boolean.TRUE.equals(profile.getIsEligible())
                    && (profile.getNextEligibleDate() == null
                    || !profile.getNextEligibleDate().isAfter(LocalDate.now()));

            if (!isStillEligible) {
                skippedCount++;
                continue;
            }

            Long donorId = user.getDonor().getId();
            if (alreadyRespondedIds.contains(donorId)) {
                skippedCount++;
                continue;
            }

            Double distance = chunk.get(user.getId());

            BloodRequestMatchNotification notification = buildNotification(bloodRequest, user, distance);

            notificationService.send(user, notification, NotificationType.BLOOD_REQUEST_MATCH);
            sentCount++;
        }

        log.debug("Chunk {} processed: sent={}, skipped={}", chunkNum, sentCount, skippedCount);
    }

    private BloodRequestMatchNotification buildNotification(BloodRequest bloodRequest, User user, Double distance) {
        boolean isCritical = bloodRequest.getUrgencyLevel() == UrgencyLevel.CRITICAL;
        String orgName = bloodRequest.getOrganization() != null
                ? bloodRequest.getOrganization().getOrgName()
                : "Hospital";
        String bloodType = bloodRequest.getBloodType().name();
        int units = bloodRequest.getUnitsNeeded();

        String title = isCritical ? "🔴 Critical Blood Donation Request" : "🩸 Blood Donation Request";
        String body = orgName + " needs " + units + " unit(s) of blood type " + bloodType;

        boolean isUnknownBloodType = user.getDonor() != null
                && user.getDonor().getHealthProfile() != null
                && user.getDonor().getHealthProfile().getBloodType() == BloodType.UNKNOWN;
        if (isUnknownBloodType) {
            body += "\nNote: Your blood type will be determined at the hospital";
        }

        if (distance != null) {
            body += " - Distance: " + String.format("%.1f", distance) + " km";
        }

        String icon = isCritical ? "heroicon-o-exclamation-triangle" : "heroicon-o-heart";
        String iconColor = isCritical ? "danger" : "primary";

        return BloodRequestMatchNotification.create(
                bloodRequest.getId(), title, body, icon, iconColor, distance);
    }
}
