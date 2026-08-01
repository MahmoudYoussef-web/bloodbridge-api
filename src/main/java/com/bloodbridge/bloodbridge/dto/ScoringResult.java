package com.bloodbridge.bloodbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ScoringResult {
    private Long donorId;
    private double score;
    private boolean coldStart;
    private String source;

    public static ScoringResult neutral(Long donorId) {
        return ScoringResult.builder()
                .donorId(donorId)
                .score(0.5)
                .coldStart(true)
                .source("neutral")
                .build();
    }

    public static ScoringResult coldStart(Long donorId) {
        return ScoringResult.builder()
                .donorId(donorId)
                .score(0.5)
                .coldStart(true)
                .source("cold_start")
                .build();
    }

    public static ScoringResult fromModel(Long donorId, double score, String source) {
        return ScoringResult.builder()
                .donorId(donorId)
                .score(score)
                .coldStart(false)
                .source(source)
                .build();
    }
}