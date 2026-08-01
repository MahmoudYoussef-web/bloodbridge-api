package com.bloodbridge.bloodbridge.service.scoring;

import com.bloodbridge.bloodbridge.service.SettingsService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
public class ScoringSettingsService {

    private static final String SCORING_GROUP = "scoring";
    private static final String EXPLORATION_RATIO_KEY = "exploration_ratio";

    private final SettingsService settingsService;

    @Value("${bloodbridge.scoring.ml-enabled:false}")
    private boolean mlScoringEnabled;

    @Value("${bloodbridge.scoring.exploration-ratio:0.20}")
    private double explorationRatio;

    @Value("${bloodbridge.scoring.max-notifications-per-broadcast:20}")
    private int maxNotificationsPerBroadcast;

    @Value("${bloodbridge.scoring.score-staleness-days:7}")
    private int scoreStalenessDays;

    @Value("${bloodbridge.scoring.min-history-for-exploitation:5}")
    private int minHistoryForExploitation;

    @Value("${bloodbridge.scoring.circuit-breaker-failure-threshold:3}")
    private int circuitBreakerFailureThreshold;

    @Value("${bloodbridge.scoring.circuit-breaker-recovery-seconds:120}")
    private int circuitBreakerRecoverySeconds;

    @Value("${bloodbridge.scoring.ml-enabled-since:}")
    private String mlEnabledSince;

    public ScoringSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void decayEpsilon() {
        double newEpsilon = 0.20;
        if (explorationRatio > 0.20) {
            explorationRatio = Math.max(0.05, explorationRatio - 0.05);
        } else if (explorationRatio > 0.15) {
            explorationRatio = 0.15;
        } else if (explorationRatio > 0.10) {
            explorationRatio = 0.10;
        } else if (explorationRatio > 0.05) {
            explorationRatio = 0.05;
        }
    }

    public void setExplorationRatio(double ratio) {
        this.explorationRatio = ratio;
        settingsService.update(SCORING_GROUP, EXPLORATION_RATIO_KEY, String.valueOf(ratio));
    }
}