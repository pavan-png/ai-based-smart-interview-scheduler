package com.interview.platform.security;

import com.interview.platform.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles generation, validation, and parsing of JWT tokens.
 * Used for both auth tokens (HR login) and action tokens (candidate email links).
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.action-token-expiration-ms}")
    private long actionTokenExpirationMs;

    /**
     * Generate a JWT token for authenticated HR user.
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date expiry = new Date(System.currentTimeMillis() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .claim("role", userPrincipal.getAuthorities().iterator().next().getAuthority())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate a secure action token for interview actions (confirm, reschedule, cancel).
     * Embeds interviewId and action type in the token.
     */
    public String generateActionToken(Long interviewId, String action) {
        Date expiry = new Date(System.currentTimeMillis() + actionTokenExpirationMs);

        return Jwts.builder()
                .setSubject("interview-action")
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .claim("interviewId", interviewId)
                .claim("action", action)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from a JWT token.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extract interviewId from action token claims.
     */
    public Long getInterviewIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("interviewId", Long.class);
    }

    /**
     * Extract action type from action token claims.
     */
    public String getActionFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("action", String.class);
    }

    /**
     * Validate a JWT token. Returns true if valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Validate an action token and throw descriptive exception on failure.
     */
    public void validateActionToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("This action link has expired. Please contact HR to resend the invitation.");
        } catch (JwtException ex) {
            throw new InvalidTokenException("This action link is invalid or has been tampered with.");
        }
    }

    /**
     * Get token expiration in milliseconds.
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    // ---- Private helpers ----

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
