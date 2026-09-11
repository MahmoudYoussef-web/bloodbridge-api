package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.bloodrequest.domain.DonationCompletedEvent;
import com.bloodbridge.bloodbridge.bloodrequest.domain.DonorAcceptedRequestEvent;
import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorHealthProfile;
import com.bloodbridge.bloodbridge.entity.Organization;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.NotificationType;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.job.CancelExcessResponsesJob;
import com.bloodbridge.bloodbridge.notification.DonorResponseNotification;
import com.bloodbridge.bloodbridge.repository.*;
import com.bloodbridge.bloodbridge.shared.audit.AuditLogService;
import com.bloodbridge.bloodbridge.shared.events.DomainEventPublisher;
import com.bloodbridge.bloodbridge.shared.monitoring.BloodBridgeMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloodRequestActionService {

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final DonorRepository donorRepository;
    private final DonorHealthProfileRepository healthProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final QRCodeService qrCodeService;
    private final CancelExcessResponsesJob cancelExcessResponsesJob;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final BloodBridgeMetrics metrics;
    private final AchievementService achievementService;
    private final DonorEligibilityService donorEligibilityService;

    private static final int MAX_ACTIVE_RESPONSES_PER_DONOR = 1;

    @Transactional
    public RequestResponse accept(User user, Long bloodRequestId, Double lat, Double lng) {
        Donor donor = donorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Donor profile not found"));

        // Pessimistic lock to prevent race conditions on the same blood request
        BloodRequest bloodRequest = bloodRequestRepository.findByIdWithPessimisticLock(bloodRequestId)
                .orElseThrow(() -> new BusinessException("Blood request not found or has been deleted"));

        if (!bloodRequest.getStatus().isActive()) {
            throw new BusinessException("This blood request is no longer active");
        }

        List<RequestResponse> allForRequest = requestResponseRepository.findByBloodRequestId(bloodRequestId);
        int unitsNeeded = bloodRequest.getUnitsNeeded() != null && bloodRequest.getUnitsNeeded() > 0
                ? bloodRequest.getUnitsNeeded() : 1;
        long admittedCount = allForRequest.stream()
                .filter(r -> r.getStatus() == RequestResponseStatus.ACCEPTED)
                .count();
        if (admittedCount >= unitsNeeded) {
            throw new BusinessException("This blood request already has enough donors");
        }

        validateDonorEligibility(donor);

        long activeCount = requestResponseRepository.countActiveClaims(
                donor.getId(), RequestResponseStatus.ACCEPTED, RequestResponseStatus.PENDING);
        if (activeCount >= MAX_ACTIVE_RESPONSES_PER_DONOR) {
            throw new BusinessException("You already have an active response to a blood request");
        }

        String qrToken = qrCodeService.generate();
        LocalDateTime qrExpiresAt = qrCodeService.calculateExpiration();

        // A broadcast offer creates a QR-less PENDING row for each matched donor.
        // Claiming that offer transitions the existing row instead of inserting
        // a duplicate (which would trip the "already responded" guard).
        Optional<RequestResponse> existingResponse =
                requestResponseRepository.findByBloodRequestIdAndDonorId(bloodRequestId, donor.getId());
        RequestResponse response;
        if (existingResponse.isPresent()) {
            RequestResponse existing = existingResponse.get();
            if (existing.getVerificationQrCode() != null
                    || existing.getStatus() != RequestResponseStatus.PENDING) {
                throw new BusinessException("You have already responded to this request");
            }
            response = existing;
        } else {
            response = new RequestResponse();
            response.setBloodRequestId(bloodRequestId);
            response.setDonorId(donor.getId());
        }

        response.setStatus(RequestResponseStatus.PENDING);
        response.setRespondedAt(LocalDateTime.now());
        response.setVerificationQrCode(qrToken);
        response.setQrCodeExpiresAt(qrExpiresAt);
        response.setLat(lat);
        response.setLng(lng);

        if (lat != null && lng != null && bloodRequest.getLat() != null && bloodRequest.getLng() != null) {
            double distance = com.bloodbridge.bloodbridge.util.GeoHelper.calculateDistance(
                    lat, lng, bloodRequest.getLat(), bloodRequest.getLng());
            response.setDistance((float) distance);
        }

        RequestResponse saved = requestResponseRepository.save(response);

        // Publish domain event
        double distance = response.getDistance() != null ? response.getDistance() : 0.0;
        eventPublisher.publish(new DonorAcceptedRequestEvent(
                saved.getId(), donor.getId(), bloodRequestId, distance));

        auditLogService.logSimple("RequestResponse", saved.getId(), "ACCEPTED", donor.getId());
        metrics.incrementQrScan();

        notifyOrganization(saved, bloodRequest);

        log.info("Donor {} accepted blood request {} (QR expires {})",
                donor.getId(), bloodRequestId, qrExpiresAt);

        return saved;
    }

    @Transactional
    public RequestResponse decline(User user, Long bloodRequestId, String reason) {
        Donor donor = donorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Donor profile not found"));

        RequestResponse response = requestResponseRepository
                .findByBloodRequestIdAndDonorId(bloodRequestId, donor.getId())
                .orElseThrow(() -> new BusinessException(
                        "No response found for this request. Only notified offers can be declined."));

        if (response.getStatus() != RequestResponseStatus.PENDING
                && response.getStatus() != RequestResponseStatus.ACCEPTED) {
            throw new BusinessException("Cannot decline a response with status " + response.getStatus());
        }

        response.setStatus(RequestResponseStatus.DECLINED);
        response.setDeclineReason(reason);
        response.setRespondedAt(LocalDateTime.now());

        return requestResponseRepository.save(response);
    }

    @Transactional
    public RequestResponse ignore(User user, Long bloodRequestId) {
        Donor donor = donorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Donor profile not found"));

        RequestResponse response = requestResponseRepository
                .findByBloodRequestIdAndDonorId(bloodRequestId, donor.getId())
                .orElseThrow(() -> new BusinessException(
                        "No response found for this request. Only notified offers can be dismissed."));

        if (response.getStatus() != RequestResponseStatus.PENDING) {
            throw new BusinessException("Can only ignore PENDING responses");
        }

        response.setStatus(RequestResponseStatus.IGNORED);
        response.setRespondedAt(LocalDateTime.now());

        return requestResponseRepository.save(response);
    }

    @Transactional
    public RequestResponse confirmAdmission(String verificationCode, Organization organization) {
        RequestResponse response = requestResponseRepository.findByVerificationQrCode(verificationCode)
                .orElseThrow(() -> new BusinessException("Invalid verification code"));

        if (!qrCodeService.validate(
                verificationCode,
                response.getVerificationQrCode(),
                response.getQrCodeExpiresAt())) {
            throw new BusinessException("QR code is invalid or expired");
        }

        BloodRequest bloodRequest = bloodRequestRepository.findByIdNotDeleted(response.getBloodRequestId())
                .orElseThrow(() -> new BusinessException("Blood request not found"));

        if (!bloodRequest.getOrganization().getId().equals(organization.getId())) {
            throw new BusinessException("This QR code belongs to a different organization");
        }

        if (!bloodRequest.getStatus().isActive()) {
            throw new BusinessException("Blood request is no longer active");
        }

        if (response.getStatus() != RequestResponseStatus.PENDING) {
            throw new BusinessException("Response cannot be confirmed (current status: " + response.getStatus() + ")");
        }

        response.setStatus(RequestResponseStatus.ACCEPTED);
        response.setVerifiedAt(LocalDateTime.now());

        log.info("Organization {} confirmed admission for donor response {} on blood request {}",
                organization.getId(), response.getId(), bloodRequest.getId());

        return requestResponseRepository.save(response);
    }

    @Transactional
    public RequestResponse complete(User user, Long responseId) {
        RequestResponse response = requestResponseRepository.findByIdWithPessimisticLock(responseId)
                .orElseThrow(() -> new BusinessException("Response not found"));

        BloodRequest bloodRequest = bloodRequestRepository.findByIdNotDeleted(response.getBloodRequestId())
                .orElseThrow(() -> new BusinessException("Blood request not found"));

        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Organization not found"));
        if (!bloodRequest.getOrganizationId().equals(org.getId())) {
            throw new BusinessException("You do not own this response", HttpStatus.FORBIDDEN);
        }

        if (response.getStatus() != RequestResponseStatus.ACCEPTED) {
            throw new BusinessException("Only ACCEPTED responses can be completed");
        }

        response.setStatus(RequestResponseStatus.COMPLETED);

        DonorHealthProfile profile = healthProfileRepository.findByDonorId(response.getDonorId())
                .orElseThrow(() -> new BusinessException("Donor health profile not found"));
        profile.setLastDonationDate(LocalDate.now());
        profile.setTotalDonations(profile.getTotalDonations() != null ? profile.getTotalDonations() + 1 : 1);

        Donor donor = donorRepository.findById(response.getDonorId())
                .orElseThrow(() -> new BusinessException("Donor not found"));
        donor.setPoints(donor.getPoints() != null ? donor.getPoints() + 10 : 10);
        if (donor.getPoints() >= 100) {
            donor.setLevel(2);
        }

        healthProfileRepository.save(profile);
        donorRepository.save(donor);

        donorEligibilityService.calculateEligibility(profile);

        RequestResponse saved = requestResponseRepository.save(response);

        // Publish domain event
        Long orgId = bloodRequest.getOrganizationId();
        eventPublisher.publish(new DonationCompletedEvent(
                saved.getId(), response.getDonorId(), response.getBloodRequestId(), orgId));

        auditLogService.logSimple("RequestResponse", saved.getId(), "COMPLETED", user.getId());
        metrics.incrementDonationComplete();

        achievementService.evaluateAndAward(response.getDonorId(), user.getId());

        return saved;
    }

    @Transactional
    public void cancelExcess(BloodRequest bloodRequest) {
        requestResponseRepository.updateAllByBloodRequestIdWhereStatus(
                bloodRequest.getId(), RequestResponseStatus.PENDING, RequestResponseStatus.NOT_NEEDED);

        cancelExcessResponsesJob.execute(bloodRequest.getId());

        log.info("CancelExcess dispatched for blood request {}", bloodRequest.getId());
    }

    /**
     * Best-effort in-app notification to the owning organization.
     * Never breaks the acceptance flow.
     */
    private void notifyOrganization(RequestResponse saved, BloodRequest bloodRequest) {
        try {
            Organization organization = organizationRepository.findById(bloodRequest.getOrganizationId())
                    .orElse(null);
            if (organization == null || organization.getUserId() == null) {
                return;
            }
            User orgUser = userRepository.findById(organization.getUserId()).orElse(null);
            if (orgUser == null) {
                return;
            }
            String bt = bloodRequest.getBloodType() != null ? bloodRequest.getBloodType().name() : "?";
            notificationService.send(orgUser,
                    new DonorResponseNotification(
                            saved.getId(),
                            bloodRequest.getId(),
                            "New donor response",
                            "A donor accepted blood request #" + bloodRequest.getId() + " (" + bt + ")",
                            "heroicon-o-check",
                            "success"),
                    NotificationType.DONOR_RESPONSE);
        } catch (Exception e) {
            log.warn("Failed to notify organization {} about response {}",
                    bloodRequest.getOrganizationId(), saved.getId(), e);
        }
    }

    private void validateDonorEligibility(Donor donor) {        DonorHealthProfile profile = healthProfileRepository.findByDonorId(donor.getId())
                .orElseThrow(() -> new BusinessException("Donor health profile not found"));

        if (Boolean.TRUE.equals(profile.getChronicDisease())) {
            throw new BusinessException("Donor is permanently ineligible due to chronic disease");
        }

        if (Boolean.FALSE.equals(profile.getIsEligible())) {
            if (profile.getNextEligibleDate() != null
                    && profile.getNextEligibleDate().isAfter(LocalDate.now())) {
                throw new BusinessException("Donor is temporarily ineligible until " + profile.getNextEligibleDate());
            }
            throw new BusinessException("Donor is currently ineligible to donate");
        }
    }
}