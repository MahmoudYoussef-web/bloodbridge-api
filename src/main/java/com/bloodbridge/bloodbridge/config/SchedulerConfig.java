package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.schedule.CleanupStaleResponses;
import com.bloodbridge.bloodbridge.schedule.DecayEpsilonCommand;
import com.bloodbridge.bloodbridge.schedule.ExpireOldBloodRequests;
import com.bloodbridge.bloodbridge.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final CleanupStaleResponses cleanupStaleResponses;
    private final ExpireOldBloodRequests expireOldBloodRequests;
    private final DecayEpsilonCommand decayEpsilonCommand;
    private final RateLimitService rateLimitService;

    @Scheduled(fixedRateString = "${bloodbridge.schedule.cleanup-stale-responses-rate:3600000}")
    public void cleanupStaleResponses() {
        log.debug("Running stale response cleanup");
        cleanupStaleResponses.execute();
    }

    @Scheduled(fixedRateString = "${bloodbridge.schedule.expire-blood-requests-rate:43200000}")
    public void expireOldBloodRequests() {
        log.debug("Running blood request expiration");
        expireOldBloodRequests.execute();
    }

    @Scheduled(cron = "${bloodbridge.schedule.decay-epsilon-cron:0 0 0 * * MON}")
    public void decayEpsilon() {
        log.debug("Running epsilon decay");
        decayEpsilonCommand.execute();
    }

    @Scheduled(fixedRateString = "${bloodbridge.schedule.rate-limiter-evict-rate:3600000}")
    public void evictStaleRateLimiterKeys() {
        log.debug("Running rate-limiter stale key eviction");
        rateLimitService.evictStaleKeys();
    }
}