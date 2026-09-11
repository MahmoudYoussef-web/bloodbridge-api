package com.bloodbridge.bloodbridge.jwt;

import com.bloodbridge.bloodbridge.shared.domain.RedisRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final ObjectProvider<RedisRateLimiter> redisRateLimiterProvider;
    private final InMemoryTokenBlacklist inMemoryTokenBlacklist;
    private final JwtService jwtService;

    public JwtBlacklistFilter(ObjectProvider<RedisRateLimiter> redisRateLimiterProvider,
                              InMemoryTokenBlacklist inMemoryTokenBlacklist,
                              JwtService jwtService) {
        this.redisRateLimiterProvider = redisRateLimiterProvider;
        this.inMemoryTokenBlacklist = inMemoryTokenBlacklist;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token) && isRevoked(token)) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"TOKEN_REVOKED\","
                        + "\"status\":401,\"detail\":\"Token has been revoked. Please log in again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRevoked(String token) {
        RedisRateLimiter limiter = redisRateLimiterProvider.getIfAvailable();
        if (limiter != null && limiter.isTokenBlacklisted(token)) {
            return true;
        }
        return inMemoryTokenBlacklist.isBlacklisted(InMemoryTokenBlacklist.hash(token));
    }
}
