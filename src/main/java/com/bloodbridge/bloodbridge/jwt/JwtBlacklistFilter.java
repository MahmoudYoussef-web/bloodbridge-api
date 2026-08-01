package com.bloodbridge.bloodbridge.jwt;

import com.bloodbridge.bloodbridge.shared.domain.RedisRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(name = "bloodbridge.redis.enabled", havingValue = "true", matchIfMissing = false)
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final RedisRateLimiter redisRateLimiter;
    private final JwtService jwtService;

    public JwtBlacklistFilter(RedisRateLimiter redisRateLimiter, JwtService jwtService) {
        this.redisRateLimiter = redisRateLimiter;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token) && redisRateLimiter.isTokenBlacklisted(token)) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token has been revoked\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
