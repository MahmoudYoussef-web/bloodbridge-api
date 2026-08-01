package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.dto.BloodRequestListView;
import com.bloodbridge.bloodbridge.dto.OrganizationProfileResponse;
import com.bloodbridge.bloodbridge.dto.OrganizationProfileUpdateRequest;
import com.bloodbridge.bloodbridge.dto.RequestResponseView;
import com.bloodbridge.bloodbridge.entity.BloodRequest;
import com.bloodbridge.bloodbridge.entity.Organization;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.exception.BusinessException;
import com.bloodbridge.bloodbridge.exception.ResourceNotFoundException;
import com.bloodbridge.bloodbridge.repository.BloodRequestRepository;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import com.bloodbridge.bloodbridge.service.BloodRequestActionService;
import com.bloodbridge.bloodbridge.service.BloodRequestBroadcastService;
import com.bloodbridge.bloodbridge.service.BloodRequestListViewAssembler;
import com.bloodbridge.bloodbridge.service.ProfileService;
import com.bloodbridge.bloodbridge.service.RateLimitService;
import com.bloodbridge.bloodbridge.service.RequestResponseViewAssembler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/org")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final BloodRequestBroadcastService broadcastService;
    private final BloodRequestActionService actionService;
    private final RateLimitService rateLimitService;
    private final ProfileService profileService;
    private final RequestResponseViewAssembler responseViewAssembler;
    private final BloodRequestListViewAssembler bloodRequestListViewAssembler;

    @GetMapping("/profile")
    public ResponseEntity<OrganizationProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getOrgProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<OrganizationProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrganizationProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateOrgProfile(user.getId(), request));
    }

    @PostMapping("/blood-requests")
    public ResponseEntity<BloodRequest> createRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BloodRequest request) {
        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        request.setOrganization(org);
        request.setOrganizationId(org.getId());
        request.setStatus(BloodRequestStatus.PENDING);

        BloodRequest saved = bloodRequestRepository.save(request);

        try {
            broadcastService.broadcast(saved);
        } catch (Exception e) {
            saved.setStatus(BloodRequestStatus.PENDING);
            saved.setBroadcastedAt(null);
            bloodRequestRepository.save(saved);
            throw new BusinessException("Failed to broadcast request: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/blood-requests")
    public ResponseEntity<List<BloodRequestListView>> getMyRequests(@AuthenticationPrincipal User user) {
        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        List<BloodRequest> requests =
                bloodRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(org.getId());
        return ResponseEntity.ok(bloodRequestListViewAssembler.assemble(requests));
    }

    @GetMapping("/blood-requests/{id}")
    public ResponseEntity<BloodRequestListView> getRequest(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        BloodRequest request = bloodRequestRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (!request.getOrganizationId().equals(org.getId())) {
            throw new BusinessException("You do not own this request", HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(
                bloodRequestListViewAssembler.assemble(List.of(request)).get(0));
    }

    @PostMapping("/blood-requests/{id}/broadcast")
    public ResponseEntity<BloodRequest> reBroadcast(@AuthenticationPrincipal User user, @PathVariable Long id) {
        BloodRequest request = bloodRequestRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (!request.getOrganization().getId().equals(org.getId())) {
            throw new BusinessException("You do not own this request");
        }

        if (request.getStatus() != BloodRequestStatus.PENDING) {
            throw new BusinessException("Only PENDING requests can be broadcasted");
        }

        broadcastService.broadcast(request);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/blood-requests/{id}/responses")
    public ResponseEntity<List<RequestResponse>> getRequestResponses(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        BloodRequest request = bloodRequestRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (!request.getOrganizationId().equals(org.getId())) {
            throw new BusinessException("You do not own this request", HttpStatus.FORBIDDEN);
        }

        List<RequestResponse> responses = requestResponseRepository.findByBloodRequestId(id);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/responses")
    public ResponseEntity<List<RequestResponseView>> getAllResponses(@AuthenticationPrincipal User user) {
        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        List<RequestResponse> rows = requestResponseRepository.findByBloodRequest_OrganizationId(org.getId());
        return ResponseEntity.ok(responseViewAssembler.assemble(rows));
    }

    @PostMapping("/scan-qr")
    public ResponseEntity<?> scanQr(
            @AuthenticationPrincipal User user,
            @RequestParam String code) {
        Organization org = organizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        String rateLimitKey = "org-" + org.getId();
        if (!rateLimitService.tryQrScan(rateLimitKey)) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Rate limit exceeded. Maximum 30 scans per minute.",
                            "retryAfter", "60s"));
        }

        RequestResponse response = actionService.confirmAdmission(code, org);
        return ResponseEntity.ok(responseViewAssembler.assemble(List.of(response)).get(0));
    }

    @PostMapping("/responses/{id}/complete")
    public ResponseEntity<RequestResponse> completeResponse(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(actionService.complete(user, id));
    }
}