package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.dto.BloodRequestCardResponse;
import com.bloodbridge.bloodbridge.dto.DonorAchievementsResponse;
import com.bloodbridge.bloodbridge.dto.DonorProfileResponse;
import com.bloodbridge.bloodbridge.dto.DonorProfileUpdateRequest;
import com.bloodbridge.bloodbridge.dto.RequestResponseView;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.exception.ResourceNotFoundException;
import com.bloodbridge.bloodbridge.repository.DonorRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import com.bloodbridge.bloodbridge.service.BloodRequestActionService;
import com.bloodbridge.bloodbridge.service.QRCodeService;
import com.bloodbridge.bloodbridge.service.ProfileService;
import com.bloodbridge.bloodbridge.service.RequestResponseViewAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/donor")
@RequiredArgsConstructor
public class DonorController {

    private final DonorRepository donorRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final BloodRequestActionService bloodRequestActionService;
    private final QRCodeService qrCodeService;
    private final ProfileService profileService;
    private final RequestResponseViewAssembler responseViewAssembler;

    @GetMapping("/blood-requests")
    public ResponseEntity<List<BloodRequestCardResponse>> getActiveRequests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getDonorActiveRequests(user.getId(), null, null));
    }

    @GetMapping("/blood-requests/{id}")
    public ResponseEntity<BloodRequestCardResponse> getRequest(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(profileService.getDonorRequestDetail(user.getId(), id));
    }

    @PostMapping("/blood-requests/{id}/accept")
    public ResponseEntity<RequestResponse> acceptRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        RequestResponse response = bloodRequestActionService.accept(user, id, lat, lng);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/blood-requests/{id}/decline")
    public ResponseEntity<RequestResponse> declineRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(bloodRequestActionService.decline(user, id, reason));
    }

    @PostMapping("/blood-requests/{id}/ignore")
    public ResponseEntity<RequestResponse> ignoreRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(bloodRequestActionService.ignore(user, id));
    }

    @GetMapping("/responses")
    public ResponseEntity<List<RequestResponseView>> getMyResponses(@AuthenticationPrincipal User user) {
        Donor donor = donorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));
        List<RequestResponse> rows =
                requestResponseRepository.findByDonorIdOrderByRespondedAtDesc(donor.getId());
        return ResponseEntity.ok(responseViewAssembler.assemble(rows));
    }

    @GetMapping("/responses/{id}/qr/download")
    public ResponseEntity<byte[]> downloadQrCode(@AuthenticationPrincipal User user, @PathVariable Long id) {
        RequestResponse response = requestResponseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Response not found"));

        if (response.getVerificationQrCode() == null) {
            throw new RuntimeException("No QR code available for this response");
        }

        byte[] qrImage = qrCodeService.generateQrImage(response.getVerificationQrCode());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr-" + id + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @GetMapping("/profile")
    public ResponseEntity<DonorProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getDonorProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<DonorProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DonorProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateDonorProfile(user.getId(), request));
    }

    @GetMapping("/achievements")
    public ResponseEntity<DonorAchievementsResponse> getAchievements(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getDonorAchievements(user.getId()));
    }
}