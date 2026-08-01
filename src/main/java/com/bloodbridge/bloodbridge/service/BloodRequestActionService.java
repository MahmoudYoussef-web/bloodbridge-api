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
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.job.CancelExcessResponsesJob;
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
        boolean alreadyHasDonor = allForRequest.stream().anyMatch(r ->
                r.getStatus() == RequestResponseStatus.PENDING || r.getStatus() == RequestResponseStatus.ACCEPTED);
        if (alreadyHasDonor) {
            throw new BusinessException("This blood request already has a donor response");
        }

        validateDonorEligibility(donor);

        long activeCount = requestResponseRepository.countByDonorIdAndStatusIn(
                donor.getId(), List.of(RequestResponseStatus.PENDING, RequestResponseStatus.ACCEPTED));
        if (activeCount >= MAX_ACTIVE_RESPONSES_PER_DONOR) {
            throw new BusinessException("You already have an active response to a blood request");
        }

        Optional<RequestResponse> existingResponse =
                requestResponseRepository.findByBloodRequestIdAndDonorId(bloodRequestId, donor.getId());
        if (existingResponse.isPresent()) {
            throw new BusinessException("You have already responded to this request");
        }

        String qrToken = qrCodeService.generate();
        LocalDateTime qrExpiresAt = qrCodeService.calculateExpiration();

        RequestResponse response = new RequestResponse();
        response.setBloodRequestId(bloodRequestId);
        response.setDonorId(donor.getId());
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

        log.info("Donor {} accepted blood request {} with QR token {} (expires {})",
                donor.getId(), bloodRequestId, qrToken, qrExpiresAt);

        return saved;
    }

    @Transactional
    public RequestResponse decline(User user, Long bloodRequestId, String reason) {
        Donor donor = donorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Donor profile not found"));

        RequestResponse response = requestResponseRepository
                .findByBloodRequestIdAndDonorId(bloodRequestId, donor.getId())
                .orElseThrow(() -> new BusinessException("Response not found"));

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
                .orElseThrow(() -> new BusinessException("Response not found"));

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

    private void validateDonorEligibility(Donor donor) {
        DonorHealthProfile profile = healthProfileRepository.findByDonorId(donor.getId())
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