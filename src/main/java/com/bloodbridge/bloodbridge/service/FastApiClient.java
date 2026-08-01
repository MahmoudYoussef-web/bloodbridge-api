package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.dto.ScoringResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class FastApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${bloodbridge.fastapi.url:http://localhost:8001}")
    private String fastApiUrl;

    @Value("${bloodbridge.fastapi.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${bloodbridge.fastapi.read-timeout:8000}")
    private int readTimeout;

    public FastApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    public Map<Long, ScoringResult> scoreDonors(List<Long> donorIds, String urgency, Map<Long, Double> distances) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("donor_ids", donorIds);
            requestBody.put("urgency", urgency != null ? urgency : "normal");
            if (distances != null && !distances.isEmpty()) {
                requestBody.put("distances", distances);
            }

            String responseBody = webClient.post()
                    .uri(fastApiUrl + "/api/score")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new RuntimeException("FastAPI /api/score returned " + response.statusCode())))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(readTimeout))
                    .block();

            if (responseBody == null) {
                log.warn("FastAPI returned empty response for /api/score");
                return new HashMap<>();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode scoresNode = root.get("scores");
            if (scoresNode == null || !scoresNode.isObject()) {
                return new HashMap<>();
            }

            Map<Long, ScoringResult> results = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = scoresNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                Long donorId = Long.parseLong(entry.getKey());
                JsonNode scoreData = entry.getValue();

                double score = scoreData.get("score").asDouble(0.5);
                boolean isColdStart = scoreData.has("is_cold_start") && scoreData.get("is_cold_start").asBoolean();

                results.put(donorId, isColdStart
                        ? ScoringResult.coldStart(donorId)
                        : ScoringResult.fromModel(donorId, score, "fastapi"));
            }

            return results;
        } catch (Exception e) {
            log.error("Failed to call FastAPI /api/score: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> triggerRetraining() {
        try {
            String responseBody = webClient.post()
                    .uri(fastApiUrl + "/api/retrain")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new RuntimeException("FastAPI /api/retrain returned " + response.statusCode())))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (responseBody == null) {
                throw new RuntimeException("Empty response from /api/retrain");
            }

            return objectMapper.readValue(responseBody, HashMap.class);
        } catch (Exception e) {
            log.error("Failed to trigger retraining: {}", e.getMessage());
            throw new RuntimeException("Failed to trigger retraining", e);
        }
    }

    public boolean isHealthy() {
        try {
            String responseBody = webClient.get()
                    .uri(fastApiUrl + "/api/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (responseBody == null) return false;

            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.has("status") ? root.get("status").asText() : "unknown";
            return "healthy".equals(status);
        } catch (Exception e) {
            log.warn("FastAPI health check failed: {}", e.getMessage());
            return false;
        }
    }
}