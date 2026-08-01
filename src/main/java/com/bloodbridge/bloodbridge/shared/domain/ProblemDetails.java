package com.bloodbridge.bloodbridge.shared.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {
    private String type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private LocalDateTime timestamp;
    private Map<String, Object> extensions;

    public static ProblemDetails of(int status, String title, String detail) {
        return ProblemDetails.builder()
                .type("about:blank")
                .title(title)
                .status(status)
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProblemDetails validationError(Map<String, Object> errors) {
        return ProblemDetails.builder()
                .type("about:blank")
                .title("Validation Error")
                .status(400)
                .detail("The request contains invalid fields")
                .timestamp(LocalDateTime.now())
                .extensions(Map.of("errors", errors))
                .build();
    }
}
