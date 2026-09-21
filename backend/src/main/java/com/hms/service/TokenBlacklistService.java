package com.hms.service;

import com.hms.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tokens that have been logged out and must no longer be accepted.
 *
 * This used to be a plain ConcurrentHashMap, which had three problems: a
 * restart un-revoked every token that had ever been logged out, a second
 * instance of the app knew nothing about the first one's logouts, and the
 * cleanup method that bounded its growth was never called by anything.
 *
 * Redis fixes all three. Each entry expires by itself when the token would
 * have expired anyway, so there is nothing to sweep.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Used only while Redis is unreachable. Revocation then still holds within
     * this instance for as long as it stays up, which is strictly better than
     * dropping it, but it is a fallback and not the store.
     */
    private final Map<String, Instant> fallback = new ConcurrentHashMap<>();

    /**
     * Revoke a token.
     *
     * The token is stored as a SHA-256 digest rather than verbatim: it is a
     * live credential until it expires, and a dump of Redis should not hand
     * anyone a set of working ones.
     */
    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        Instant expiry = jwtTokenProvider.getExpiry(token).orElse(null);
        Duration ttl = remainingLife(expiry);

        // Already expired: the filter rejects it on its own signature check,
        // so there is nothing to remember.
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        String key = KEY_PREFIX + digest(token);
        try {
            redisTemplate.opsForValue().set(key, "1", ttl);
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, revoking token in memory only: {}", e.getMessage());
            fallback.put(key, expiry == null ? Instant.now().plus(ttl) : expiry);
        }
    }

    /**
     * True when this token has been logged out.
     *
     * If Redis cannot be reached this answers from the in-memory fallback
     * instead of failing the request. A cache outage should not sign everyone
     * out; the cost is that a logout recorded on another instance may be
     * missed until Redis returns.
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String key = KEY_PREFIX + digest(token);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, checking the in-memory revocation list: {}", e.getMessage());
            return isInFallback(key);
        }
    }

    private boolean isInFallback(String key) {
        Instant expiry = fallback.get(key);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            fallback.remove(key);
            return false;
        }
        return true;
    }

    private Duration remainingLife(Instant expiry) {
        if (expiry == null) {
            // Unreadable token. Hold it for a day, which is the configured
            // token lifetime, rather than dropping the revocation entirely.
            return Duration.ofDays(1);
        }
        return Duration.between(Instant.now(), expiry);
    }

    private String digest(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every JVM; this cannot happen.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Size of the in-memory fallback only - Redis entries are not counted. */
    public int getFallbackSize() {
        return fallback.size();
    }

    /** Drops the in-memory fallback. Redis entries expire on their own. */
    public void clearFallback() {
        fallback.clear();
    }

    Optional<Instant> fallbackExpiryOf(String token) {
        return Optional.ofNullable(fallback.get(KEY_PREFIX + digest(token)));
    }
}
