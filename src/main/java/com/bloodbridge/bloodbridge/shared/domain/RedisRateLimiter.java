package com.bloodbridge.bloodbridge.shared.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "bloodbridge.redis.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long now = Instant.now().getEpochSecond();
        Long windowStart = now - windowSeconds;

        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        Long count = redisTemplate.opsForZSet().size(redisKey);
        if (count != null && count >= maxRequests) {
            return false;
        }
        redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
        redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        return true;
    }

    public long getRemaining(String key, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long now = Instant.now().getEpochSecond();
        Long windowStart = now - windowSeconds;

        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        Long count = redisTemplate.opsForZSet().size(redisKey);
        long used = count != null ? count : 0;
        return Math.max(0, maxRequests - used);
    }

    public void blacklistToken(String token, long ttlSeconds) {
        String redisKey = "blacklist:jwt:" + token;
        redisTemplate.opsForValue().set(redisKey, "true", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        String redisKey = "blacklist:jwt:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }
}
