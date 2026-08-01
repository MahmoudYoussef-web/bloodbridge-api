package com.bloodbridge.bloodbridge.schedule;

import com.bloodbridge.bloodbridge.service.scoring.ScoringSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecayEpsilonCommand {

    private final ScoringSettingsService scoringSettingsService;

    public void execute() {
        double currentEpsilon = scoringSettingsService.getExplorationRatio();

        if (currentEpsilon <= 0.01) {
            log.info("Epsilon already at minimum ({}). No decay applied.", currentEpsilon);
            return;
        }

        double newEpsilon = Math.max(0.01, currentEpsilon / 2.0);

        scoringSettingsService.setExplorationRatio(newEpsilon);

        log.info("Epsilon decayed from {} to {} (min={})", currentEpsilon, newEpsilon, 0.01);
    }
}