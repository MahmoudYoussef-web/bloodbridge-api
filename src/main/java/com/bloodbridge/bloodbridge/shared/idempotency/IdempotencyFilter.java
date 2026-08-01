package com.bloodbridge.bloodbridge.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class IdempotencyFilter implements Filter {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int IDEMPOTENCY_TTL_HOURS = 24;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String idempotencyKey = httpRequest.getHeader(IDEMPOTENCY_HEADER);

        if (!HttpMethod.POST.matches(method) && !HttpMethod.PUT.matches(method) && !HttpMethod.PATCH.matches(method)) {
            chain.doFilter(request, response);
            return;
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            if (key.getExpiresAt().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.delete(key);
            } else {
                log.info("Idempotency key '{}' replayed for {} {}", idempotencyKey, method, httpRequest.getRequestURI());
                httpResponse.setStatus(key.getResponseStatus());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(key.getResponseBody());
                return;
            }
        }

        IdempotencyKey newKey = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .httpMethod(method)
                .requestPath(httpRequest.getRequestURI())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(IDEMPOTENCY_TTL_HOURS))
                .build();
        idempotencyKeyRepository.save(newKey);

        try {
            chain.doFilter(request, response);
        } finally {
            if (httpResponse.getStatus() >= 200 && httpResponse.getStatus() < 500) {
                newKey.setResponseStatus(httpResponse.getStatus());
                newKey.setResponseBody("{\"status\": " + httpResponse.getStatus() + "}");
                idempotencyKeyRepository.save(newKey);
            } else {
                idempotencyKeyRepository.delete(newKey);
            }
        }
    }
}
