package com.bloodbridge.bloodbridge.service.scoring;

import com.bloodbridge.bloodbridge.dto.ScoringResult;
import com.bloodbridge.bloodbridge.entity.Donor;
import com.bloodbridge.bloodbridge.entity.DonorPredictiveScore;
import com.bloodbridge.bloodbridge.repository.DonorPredictiveScoreRepository;
import com.bloodbridge.bloodbridge.repository.ModelTrainingLogRepository;
import com.bloodbridge.bloodbridge.repository.RequestResponseRepository;
import com.bloodbridge.bloodbridge.service.FastApiClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorScoringServiceTest {

    @Mock private ScoringSettingsService settings;
    @Mock private FastApiCircuitBreaker circuitBreaker;
    @Mock private DonorPredictiveScoreRepository predictiveScoreRepository;
    @Mock private RequestResponseRepository requestResponseRepository;
    @Mock private ModelTrainingLogRepository modelTrainingLogRepository;
    @Mock private EntityManager entityManager;
    @Mock private FastApiClient fastApiClient;

    private DonorScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new DonorScoringService(settings, circuitBreaker,
                predictiveScoreRepository, requestResponseRepository,
                modelTrainingLogRepository, entityManager, fastApiClient);
    }

    @Test
    void shouldReturnColdStartForNewDonors() {
        Donor donor = Donor.builder().id(1L).build();

        when(predictiveScoreRepository.findFreshScoresByDonorIds(anyList(), any()))
                .thenReturn(List.of());
        when(settings.isMlScoringEnabled()).thenReturn(false);

        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        DonorScoringService.ScoreSelectionResult result =
                scoringService.scoreAndSelect(List.of(donor), "normal");

        assertThat(result).isNotNull();
        assertThat(result.getSelected()).isEmpty();
    }

    @Test
    void shouldScoreDonorsFromDbCache() {
        when(predictiveScoreRepository.findFreshScoresByDonorIds(anyList(), any()))
                .thenReturn(List.of(
                        DonorPredictiveScore.builder()
                                .donorId(1L)
                                .acceptanceProbability(0.85)
                                .computedAt(LocalDateTime.now())
                                .build()
                ));

        Map<Long, ScoringResult> results = scoringService.getScoreResults(
                List.of(1L), "normal", Map.of());

        assertThat(results).containsKey(1L);
        assertThat(results.get(1L).getScore()).isEqualTo(0.85);
        assertThat(results.get(1L).getSource()).isEqualTo("db_cache");
    }

    @Test
    void shouldHandleEmptyDonorList() {
        List<Donor> donors = List.of();

        DonorScoringService.ScoreSelectionResult result =
                scoringService.scoreAndSelect(donors, "normal");

        assertThat(result).isNotNull();
        assertThat(result.getSelected()).isEmpty();
    }

    @Test
    void shouldHandleCriticalUrgency() {
        Donor donor1 = Donor.builder().id(1L).build();

        when(predictiveScoreRepository.findFreshScoresByDonorIds(anyList(), any()))
                .thenReturn(List.of(
                        DonorPredictiveScore.builder()
                                .donorId(1L)
                                .acceptanceProbability(0.9)
                                .computedAt(LocalDateTime.now())
                                .build()
                ));

        when(settings.getMaxNotificationsPerBroadcast()).thenReturn(20);
        when(settings.getExplorationRatio()).thenReturn(0.2);
        when(settings.getScoreStalenessDays()).thenReturn(7);

        DonorScoringService.ScoreSelectionResult result =
                scoringService.scoreAndSelect(List.of(donor1), "critical");

        assertThat(result).isNotNull();
    }

    @Test
    void shouldPersistToDbCache() {
        when(predictiveScoreRepository.findFreshScoresByDonorIds(anyList(), any()))
                .thenReturn(List.of());
        when(settings.isMlScoringEnabled()).thenReturn(false);

        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        when(modelTrainingLogRepository.findLatestTrainingLog())
                .thenReturn(java.util.Optional.empty());

        Map<Long, ScoringResult> freshScores = scoringService.getScoreResults(
                List.of(1L), "normal", Map.of());

        assertThat(freshScores).isNotNull();
    }
}
