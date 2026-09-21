package com.hms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Caps how often one source may call the unauthenticated endpoints.
 *
 * Only /api/auth/login and /api/auth/signup are covered. Everything else
 * already needs a valid token, and a token can be revoked; these two are the
 * ones anybody on the internet can call as often as they like.
 *
 * This is the per-source half of the protection. LoginAttemptService is the
 * per-account half, which is what stops a distributed attempt against one
 * user. Neither replaces the other.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "ratelimit:";

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/signup");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${hms.security.rate-limit.requests:20}")
    private int maxRequests;

    @Value("${hms.security.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${hms.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = clientIp(request);
        long count = countRequest(ip, request.getRequestURI());

        if (count > maxRequests) {
            log.warn("Rate limit exceeded: ip={} path={} count={}", ip, request.getRequestURI(), count);
            reject(response);
            return;
        }

        // Let the caller see where they stand rather than hitting a wall
        // without warning.
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - count)));

        filterChain.doFilter(request, response);
    }

    /**
     * A fixed window per IP and path: the first request of a window sets the
     * expiry, and the counter resets when it lapses.
     *
     * If Redis is unreachable this returns 0, so the request is allowed. An
     * outage must not take sign-in down with it; the same trade-off the cache
     * and the lockout make.
     */
    private long countRequest(String ip, String path) {
        String key = KEY_PREFIX + path + ":" + ip;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count == null ? 0 : count;
        } catch (RuntimeException e) {
            log.warn("Could not apply the rate limit, letting the request through: {}", e.getMessage());
            return 0;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        objectMapper.writeValue(response.getWriter(),
                new ApiResponse("Too many requests. Please wait and try again.", false));
    }

    /**
     * The originating address, honouring X-Forwarded-For because in every
     * deployment of this app there is a proxy in front and the socket address
     * would otherwise be the proxy's.
     *
     * The leftmost entry is the closest thing to the real client. It is also
     * client-supplied and therefore forgeable - which is acceptable here,
     * since the per-account lockout does not depend on it.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
