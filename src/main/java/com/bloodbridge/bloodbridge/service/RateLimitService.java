package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.config.InMemoryRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitService {

    private final InMemoryRateLimiter qrScanLimiter = new InMemoryRateLimiter(30, 60);
    private final InMemoryRateLimiter contactLimiter = new InMemoryRateLimiter(3, 60);
    private final InMemoryRateLimiter emailVerificationLimiter = new InMemoryRateLimiter(6, 60);

    private final ConcurrentMap<String, InMemoryRateLimiter> endpointLimiters = new ConcurrentHashMap<>();

    public boolean tryQrScan(String orgIdentifier) {
        return qrScanLimiter.tryAcquire("qr:" + orgIdentifier);
    }

    public int getQrScanRemaining(String orgIdentifier) {
        return qrScanLimiter.getRemaining("qr:" + orgIdentifier);
    }

    public boolean tryContactSubmission(String ip) {
        return contactLimiter.tryAcquire("contact:" + ip);
    }

    public boolean tryEmailVerification(String email) {
        return emailVerificationLimiter.tryAcquire("verify:" + email);
    }

    public boolean checkEndpointLimit(String endpointKey, String clientId, int maxRequests, int windowSeconds) {
        String limiterKey = maxRequests + ":" + windowSeconds;
        InMemoryRateLimiter limiter = endpointLimiters.computeIfAbsent(
                limiterKey, k -> new InMemoryRateLimiter(maxRequests, windowSeconds));
        return limiter.tryAcquire(endpointKey + ":" + clientId);
    }

    public void evictStaleKeys() {
        qrScanLimiter.evictStaleKeys();
        contactLimiter.evictStaleKeys();
        emailVerificationLimiter.evictStaleKeys();
        endpointLimiters.values().forEach(InMemoryRateLimiter::evictStaleKeys);
    }
}
