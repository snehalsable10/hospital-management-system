package com.hms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Generate JWT token for user
     * @param userId - user id
     * @param username - username
     * @param role - user role
     * @return JWT token string
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                // A unique id per token.
                //
                // Without it the claims are subject, username, role, issued-at
                // and expiry - and the two timestamps are second-granularity,
                // so two sign-ins by the same user inside one second produced
                // byte-identical tokens. Logging one session out then revoked
                // the other, because the blacklist had no way to tell them
                // apart.
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Get user ID from token
     * @param token - JWT token
     * @return user id
     */
    public Long getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Get username from token
     * @param token - JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("username");
    }

    /**
     * Get role from token
     * @param token - JWT token
     * @return user role
     */
    public String getRoleFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("role");
    }

    /**
     * Validate JWT token
     * @param token - JWT token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            System.err.println("Expired JWT token: " + ex.getMessage());
            return false;
        } catch (UnsupportedJwtException ex) {
            System.err.println("Unsupported JWT token: " + ex.getMessage());
            return false;
        } catch (MalformedJwtException ex) {
            System.err.println("Invalid JWT token: " + ex.getMessage());
            return false;
        } catch (SignatureException ex) {
            System.err.println("JWT signature validation failed: " + ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT claims string is empty: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Extract token from Authorization header
     * @param authHeader - Authorization header value
     * @return JWT token (without "Bearer " prefix)
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }


    /**
     * When this token stops being valid.
     *
     * The blacklist uses it as a time-to-live: there is no point keeping a
     * revoked token on file for longer than it would have been accepted
     * anyway.
     *
     * @return the expiry instant, or empty if the token cannot be read
     */
    public Optional<Instant> getExpiry(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration == null ? Optional.empty() : Optional.of(expiration.toInstant());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}