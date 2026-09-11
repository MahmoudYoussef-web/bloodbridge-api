package com.bloodbridge.bloodbridge.dto;

import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Flat admin projection of a donation response. Built by a constructor
 * query, so no entity (and no QR token) ever leaves the server here.
 */
@Getter
public class AdminResponseView {
    private final Long id;
    private final Long bloodRequestId;
    private final Long donorId;
    private final String donorName;
    private final String organizationName;
    private final Integer status;
    private final LocalDateTime respondedAt;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime createdAt;

    public AdminResponseView(Long id, Long bloodRequestId, Long donorId,
                             String donorName, String organizationName,
                             RequestResponseStatus status,
                             LocalDateTime respondedAt, LocalDateTime verifiedAt,
                             LocalDateTime createdAt) {
        this.id = id;
        this.bloodRequestId = bloodRequestId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.organizationName = organizationName;
        this.status = status != null ? status.getValue() : null;
        this.respondedAt = respondedAt;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
    }
}
