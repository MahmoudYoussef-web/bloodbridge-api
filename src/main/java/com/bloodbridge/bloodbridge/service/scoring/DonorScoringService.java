package com.bloodbridge.bloodbridge.service.scoring;

import com.bloodbridge.bloodbridge.dto.ScoringResult;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorPredictiveScore;
import com.bloodbridge.bloodbridge.entity.RequestResponse;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.repository.DonorPredictiveScoreRepository;
import com.bloodbridge.bloodbridge.repository.ModelTrainingLogRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonorScoringService {

    private final ScoringSettingsService settings;
    private final FastApiCircuitBreaker circuitBreaker;
    private final DonorPredictiveScoreRepository predictiveScoreRepository;
    private final RequestResponseRepository requestResponseRepository;
    private final ModelTrainingLogRepository modelTrainingLogRepository;
    private final EntityManager entityManager;
    private final com.bloodbridge.bloodbridge.service.FastApiClient fastApiClient;

    /**
     * Main entry point. Scores donors and applies epsilon-greedy selection.
     */
    public ScoreSelectionResult scoreAndSelect(List<Donor> donors, String urgency) {
        Map<Long, ScoringResult> results = getScoreResults(
                donors.stream().map(Donor::getId).collect(Collectors.toList()),
                urgency,
                new HashMap<>()
        );

        List<ScoredDonor> scored = new ArrayList<>();
        for (Donor donor : donors) {
            ScoringResult result = results.getOrDefault(donor.getId(), ScoringResult.neutral(donor.getId()));
            scored.add(new ScoredDonor(donor, result));
        }

        return splitByEpsilonGreedy(scored, urgency);
    }

    @Transactional
    public Map<Long, ScoringResult> getScoreResults(List<Long> donorIds, String urgency, Map<Long, Double> distances) {
        Map<Long, ScoringResult> results = new HashMap<>();

        List<Long> remaining = new ArrayList<>(donorIds);

        Map<Long, ScoringResult> dbResults = getFromDbCache(remaining);
        results.putAll(dbResults);
        remaining.removeAll(results.keySet());

        if (remaining.isEmpty()) {
            return results;
        }

        if (settings.isMlScoringEnabled()) {
            Map<Long, ScoringResult> apiResults = getFromFastApi(remaining, urgency, distances);
            results.putAll(apiResults);
            remaining.removeAll(results.keySet());
        }

        if (remaining.isEmpty()) {
            persistToDbCache(results);
            return results;
        }

        log.info("Using rule-based scoring for {} donors", remaining.size());
        Map<Long, ScoringResult> ruleResults = getFromRuleBasedQuery(remaining);
        results.putAll(ruleResults);

        persistToDbCache(results);

        return results;
    }

    private Map<Long, ScoringResult> getFromDbCache(List<Long> donorIds) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(settings.getScoreStalenessDays());
        List<DonorPredictiveScore> scores = predictiveScoreRepository.findFreshScoresByDonorIds(donorIds, threshold);

        Map<Long, ScoringResult> results = new HashMap<>();
        for (DonorPredictiveScore score : scores) {
            results.put(score.getDonorId(), ScoringResult.fromModel(
                    score.getDonorId(),
                    score.getAcceptanceProbability(),
                    "db_cache"
            ));
        }
        return results;
    }

    private Map<Long, ScoringResult> getFromFastApi(List<Long> donorIds, String urgency, Map<Long, Double> distances) {
        try {
            Map<Long, ScoringResult> apiResults = circuitBreaker.attempt(() ->
                    fastApiClient.scoreDonors(donorIds, urgency, distances));

            if (apiResults == null) {
                return new HashMap<>();
            }
            return apiResults;
        } catch (Exception e) {
            log.warn("FastAPI scoring failed: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, ScoringResult> getFromRuleBasedQuery(List<Long> donorIds) {
        int minHistory = settings.getMinHistoryForExploitation();

        String jpql = """
            SELECT d.id, COUNT(rr.id) as totalResponses,
                   SUM(CASE WHEN rr.status IN :acceptedStatuses THEN 1 ELSE 0 END) as acceptedCount,
                   SUM(CASE WHEN rr.status = :noShowStatus THEN 1 ELSE 0 END) as noShowCount,
                   MAX(rr.respondedAt) as lastRespondedAt,
                   COALESCE(dhp.totalDonations, 0) as totalDonations
            FROM Donor d
            LEFT JOIN RequestResponse rr ON d.id = rr.donorId AND rr.status IN :countedStatuses AND rr.respondedAt IS NOT NULL
            LEFT JOIN d.healthProfile dhp
            WHERE d.id IN :donorIds AND d.deletedAt IS NULL
            GROUP BY d.id, dhp.totalDonations
            """;

        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class)
                .setParameter("donorIds", donorIds)
                .setParameter("acceptedStatuses", List.of(
                        RequestResponseStatus.ACCEPTED, RequestResponseStatus.COMPLETED))
                .setParameter("noShowStatus", RequestResponseStatus.NO_SHOW)
                .setParameter("countedStatuses", List.of(
                        RequestResponseStatus.PENDING, RequestResponseStatus.ACCEPTED,
                        RequestResponseStatus.COMPLETED, RequestResponseStatus.IGNORED,
                        RequestResponseStatus.NO_SHOW, RequestResponseStatus.UNREACHABLE,
                        RequestResponseStatus.NOT_NEEDED))
                .getResultList();

        Map<Long, ScoringResult> results = new HashMap<>();

        for (Object[] row : rows) {
            Long donorId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            long acceptedCount = ((Number) row[2]).longValue();
            long noShowCount = ((Number) row[3]).longValue();
            LocalDateTime lastRespondedAt = (LocalDateTime) row[4];
            long totalDonations = ((Number) row[5]).longValue();

            if (total < minHistory) {
                results.put(donorId, ScoringResult.coldStart(donorId));
                continue;
            }

            long noShowPenalty = noShowCount;
            long adjustedTotal = total + noShowPenalty;
            double acceptanceRate = adjustedTotal > 0 ? (double) acceptedCount / adjustedTotal : 0;

            long daysSinceLast = lastRespondedAt != null
                    ? ChronoUnit.DAYS.between(lastRespondedAt, LocalDateTime.now())
                    : 999;

            double recencyScore;
            if (daysSinceLast <= 7) recencyScore = 1.0;
            else if (daysSinceLast <= 30) recencyScore = 0.8;
            else if (daysSinceLast <= 90) recencyScore = 0.5;
            else if (daysSinceLast <= 180) recencyScore = 0.3;
            else recencyScore = 0.1;

            double loyaltyScore = Math.min((double) totalDonations / 10, 1.0);

            double score = Math.round(
                    (acceptanceRate * 0.50 + recencyScore * 0.30 + loyaltyScore * 0.20) * 10000.0
            ) / 10000.0;

            results.put(donorId, ScoringResult.fromModel(donorId, score, "rule_based"));
        }

        return results;
    }

    @Transactional
    protected void persistToDbCache(Map<Long, ScoringResult> results) {
        String latestModelVersion = modelTrainingLogRepository.findLatestTrainingLog()
                .map(log -> log.getModelVersion())
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Long, ScoringResult> entry : results.entrySet()) {
            ScoringResult result = entry.getValue();
            if (result.isColdStart() || "db_cache".equals(result.getSource())) {
                continue;
            }

            String modelVersion = switch (result.getSource()) {
                case "fastapi" -> latestModelVersion != null ? latestModelVersion : "fastapi";
                case "rule_based" -> "rule_based";
                default -> result.getSource();
            };

            DonorPredictiveScore score = DonorPredictiveScore.builder()
                    .donorId(entry.getKey())
                    .acceptanceProbability(result.getScore())
                    .computedAt(now)
                    .modelVersion(modelVersion)
                    .dataPointsCount(0)
                    .build();

            predictiveScoreRepository.save(score);
        }
    }

    private ScoreSelectionResult splitByEpsilonGreedy(List<ScoredDonor> scored, String urgency) {
        List<ScoredDonor> coldStarts = scored.stream()
                .filter(s -> s.result.isColdStart())
                .collect(Collectors.toList());

        List<ScoredDonor> withScores = scored.stream()
                .filter(s -> !s.result.isColdStart())
                .sorted(Comparator.comparingDouble(s -> -s.result.getScore()))
                .collect(Collectors.toList());

        double epsilon = settings.getExplorationRatio();
        int exploreCount = (int) Math.ceil(withScores.size() * epsilon);

        List<ScoredDonor> exploiters = withScores.subList(0, withScores.size() - exploreCount);
        List<ScoredDonor> lowScorers = withScores.subList(withScores.size() - exploreCount, withScores.size());

        List<ScoredDonor> explorers = new ArrayList<>(coldStarts);
        explorers.addAll(lowScorers);

        int budget = settings.getMaxNotificationsPerBroadcast();
        if ("critical".equalsIgnoreCase(urgency)) {
            budget = (int) (budget * 1.5);
        }

        int exploitSlots = (int) Math.ceil(budget * (1 - epsilon));
        int exploreSlots = budget - exploitSlots;

        Collections.shuffle(explorers);

        List<ScoredDonor> selected = new ArrayList<>();
        selected.addAll(exploiters.stream().limit(exploitSlots).collect(Collectors.toList()));
        selected.addAll(explorers.stream().limit(exploreSlots).collect(Collectors.toList()));

        int remainingBudget = budget - selected.size();
        if (remainingBudget > 0) {
            Set<Long> selectedIds = selected.stream().map(s -> s.donor.getId()).collect(Collectors.toSet());
            List<ScoredDonor> backfill = new ArrayList<>(exploiters);
            backfill.addAll(explorers);
            backfill = backfill.stream()
                    .filter(s -> !selectedIds.contains(s.donor.getId()))
                    .limit(remainingBudget)
                    .collect(Collectors.toList());
            selected.addAll(backfill);
        }

        long coldStartCount = scored.stream().filter(s -> s.result.isColdStart()).count();
        Map<String, Long> sourceBreakdown = scored.stream()
                .collect(Collectors.groupingBy(s -> s.result.getSource(), Collectors.counting()));

        long selectedExplorerCount = selected.stream()
                .filter(s -> explorers.stream().anyMatch(e -> e.donor.getId().equals(s.donor.getId())))
                .count();
        long selectedExploiterCount = selected.size() - selectedExplorerCount;

        log.info("DonorScoringService::scoreAndSelect - total: {}, exploiters: {}, explorers: {}, selected: {}, coldStart: {}",
                scored.size(), exploiters.size(), explorers.size(), selected.size(), coldStartCount);

        return ScoreSelectionResult.builder()
                .selected(selected.stream().map(s -> s.donor).collect(Collectors.toList()))
                .exploiterCount((int) selectedExploiterCount)
                .explorerCount((int) selectedExplorerCount)
                .coldStartCount((int) coldStartCount)
                .sourceBreakdown(sourceBreakdown)
                .build();
    }

    @lombok.Value
    @lombok.Builder
    public static class ScoreSelectionResult {
        List<Donor> selected;
        int exploiterCount;
        int explorerCount;
        int coldStartCount;
        Map<String, Long> sourceBreakdown;
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class ScoredDonor {
        private Donor donor;
        private ScoringResult result;
    }
}