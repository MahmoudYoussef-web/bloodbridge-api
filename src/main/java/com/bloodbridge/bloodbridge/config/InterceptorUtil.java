package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.shared.domain.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * Interceptors run before controllers, so they cannot use
 * {@code @RestControllerAdvice}. This helper renders the same
 * RFC-7807 {@link ProblemDetails} JSON shape so clients (and the
 * frontend) can handle interceptor denials exactly like API errors.
 */
public final class InterceptorUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private InterceptorUtil() {
    }

    public static boolean deny(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        ProblemDetails problem = ProblemDetails.of(status.value(), code, detail);
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(), problem);
        return false;
    }
}
