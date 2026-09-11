package com.bloodbridge.bloodbridge.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback revocation store used when Redis is disabled (local/dev profiles).
 * Previously logout/refresh-rotation silently did nothing without Redis while
 * reporting success. Tokens are keyed by SHA-256 hash, never raw values.
 */
@Slf4j
@Component
public class InMemoryTokenBlacklist {

    private final ConcurrentHashMap<String, Long> revoked = new ConcurrentHashMap<>();

    /** SHA-256 hex digest so raw tokens are never used as store keys. */
    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }

    public void blacklist(String tokenHash, long ttlSeconds) {
        revoked.put(tokenHash, System.currentTimeMillis() + ttlSeconds * 1000);
    }

    public boolean isBlacklisted(String tokenHash) {
        Long expiresAt = revoked.get(tokenHash);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            revoked.remove(tokenHash);
            return false;
        }
        return true;
    }

    @Scheduled(fixedRateString = "${bloodbridge.schedule.rate-limiter-evict-rate:3600000}")
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, Long> entry : revoked.entrySet()) {
            if (entry.getValue() <= now) {
                revoked.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Purged {} expired token blacklist entries", removed);
        }
    }
}
