package com.bloodbridge.bloodbridge.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtService {

    @Value("${bloodbridge.jwt.secret}")
    private String secretKey;

    @Value("${bloodbridge.jwt.expiration}")
    private long jwtExpiration;

    @Value("${bloodbridge.jwt.refresh-expiration}")
    private long refreshExpiration;

    @PostConstruct
    void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("bloodbridge.jwt.secret must be set (base64 or plain text, min 16 chars)");
        }
        if (secretKey.length() < 16) {
            throw new IllegalStateException("bloodbridge.jwt.secret is too short, use at least 16 characters");
        }
        if (secretKey.startsWith("YXNkZmdoamts")) {
            log.warn("SECURITY: using the built-in default JWT secret. Set JWT_SECRET env var in production!");
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractType(token)) && isTokenValid(token);
        } catch (Exception e) {
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("type", "access");
        return buildToken(claims, email, jwtExpiration);
    }

    public String generateRefreshToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("type", "refresh");
        return buildToken(claims, email, refreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getRemainingSeconds(String token) {
        try {
            long millis = extractExpiration(token).getTime() - System.currentTimeMillis();
            return Math.max(0, millis / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    public Date getExpirationDate(String token) {
        return extractExpiration(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            // Accept plain-text secrets by deriving a 256-bit key (SHA-256).
            // Previously any non-base64 secret crashed every auth call with a 500.
            try {
                keyBytes = MessageDigest.getInstance("SHA-256")
                        .digest(secretKey.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("Cannot derive JWT signing key", ex);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}