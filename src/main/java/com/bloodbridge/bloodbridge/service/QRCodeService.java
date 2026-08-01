package com.bloodbridge.bloodbridge.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
public class QRCodeService {

    private static final int TOKEN_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${bloodbridge.qrcode.expiration-hours:168}")
    private long expirationHours;

    @Value("${bloodbridge.qrcode.size:300}")
    private int qrSize;

    public String generate() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return HexFormat.of().formatHex(tokenBytes);
    }

    public byte[] generateQrImage(String token) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(token, BarcodeFormat.QR_CODE, qrSize, qrSize);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code image: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    public boolean validate(String token, String storedToken, LocalDateTime expiresAt) {
        if (token == null || storedToken == null || !token.equals(storedToken)) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            log.warn("QR code token expired at {}", expiresAt);
            return false;
        }
        return true;
    }

    public boolean validate(String token, String storedToken, LocalDateTime expiresAt, Long organizationId, Long responseOrgId) {
        if (!validate(token, storedToken, expiresAt)) {
            return false;
        }
        if (!organizationId.equals(responseOrgId)) {
            log.warn("QR code organization mismatch: expected {} got {}", responseOrgId, organizationId);
            return false;
        }
        return true;
    }

    public LocalDateTime calculateExpiration() {
        return LocalDateTime.now().plusHours(expirationHours);
    }
}