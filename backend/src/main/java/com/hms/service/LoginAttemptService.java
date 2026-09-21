package com.hms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * Locks an account after repeated failed sign-ins.
 *
 * Without this, nothing at all stood between an attacker and an unlimited
 * number of password guesses against a known email address.
 *
 * Counted per account rather than per IP, because that is what protects a
 * specific user from a distributed guessing attempt. The per-IP limit in
 * RateLimitFilter covers the other direction - one source spraying many
 * accounts - and the two are meant to be used together.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private static final String KEY_PREFIX = "auth:failures:";

    private final StringRedisTemplate redisTemplate;

    @Value("${hms.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${hms.security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    /**
     * True when this account has failed too many times recently and should be
     * refused without even checking the password.
     *
     * If Redis is unreachable this returns false: an outage must not lock
     * everybody out of the application. The cost is that lockout stops being
     * enforced while Redis is down, which is the same trade-off the cache
     * makes elsewhere.
     */
    public boolean isLocked(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        try {
            String count = redisTemplate.opsForValue().get(key(email));
            return count != null && Integer.parseInt(count) >= maxAttempts;
            // RuntimeException covers both a Redis failure and a counter that
            // somehow is not a number.
        } catch (RuntimeException e) {
            log.warn("Could not read the failed-login count, letting the attempt through: {}",
                    e.getMessage());
            return false;
        }
    }

    /**
     * Record a failed attempt.
     *
     * The window is a sliding one: the expiry is pushed out on every failure,
     * so an attacker cannot simply pace their guesses to stay under it.
     *
     * @return the number of failures now on record, or 0 if it could not be counted
     */
    public long recordFailure(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        try {
            String key = key(email);
            Long count = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofMinutes(lockoutMinutes));
            return count == null ? 0 : count;
        } catch (RuntimeException e) {
            log.warn("Could not record the failed login: {}", e.getMessage());
            return 0;
        }
    }

    /** Clears the count. Called on a successful sign-in. */
    public void reset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(key(email));
        } catch (RuntimeException e) {
            log.warn("Could not clear the failed-login count: {}", e.getMessage());
        }
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getLockoutMinutes() {
        return lockoutMinutes;
    }

    private String key(String email) {
        return KEY_PREFIX + email.trim().toLowerCase(Locale.ROOT);
    }
}
