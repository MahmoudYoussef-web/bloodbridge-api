package com.bloodbridge.bloodbridge.shared.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldCreateProblemDetails() {
        ProblemDetails problem = ProblemDetails.of(400, "BAD_REQUEST", "Invalid input");

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("BAD_REQUEST");
        assertThat(problem.getDetail()).isEqualTo("Invalid input");
        assertThat(problem.getType()).isEqualTo("about:blank");
        assertThat(problem.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateValidationError() {
        ProblemDetails problem = ProblemDetails.validationError(
                Map.of("email", "must be a valid email")
        );

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
        assertThat(problem.getExtensions()).isNotNull();
        assertThat(problem.getExtensions().get("errors")).isInstanceOf(Map.class);
    }

    @Test
    void shouldSerializeToJson() throws JsonProcessingException {
        ProblemDetails problem = ProblemDetails.of(401, "UNAUTHORIZED", "Invalid token");

        String json = objectMapper.writeValueAsString(problem);
        assertThat(json).contains("\"status\":401");
        assertThat(json).contains("\"title\":\"UNAUTHORIZED\"");
        assertThat(json).contains("\"detail\":\"Invalid token\"");
    }

    @Test
    void shouldExcludeNullFields() throws JsonProcessingException {
        ProblemDetails problem = ProblemDetails.of(200, "OK", "Success");

        String json = objectMapper.writeValueAsString(problem);
        assertThat(json).doesNotContain("\"extensions\"");
        assertThat(json).doesNotContain("\"instance\"");
    }
}
