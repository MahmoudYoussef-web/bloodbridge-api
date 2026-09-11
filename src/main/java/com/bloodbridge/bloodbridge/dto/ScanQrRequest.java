package com.bloodbridge.bloodbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * QR admission code. Sent in the request body (never in the URL) so the
 * bearer-equivalent token is not written to access logs.
 */
public record ScanQrRequest(@NotBlank(message = "Code is required") String code) {
}
