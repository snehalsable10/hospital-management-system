package com.hms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The per-source half of the sign-in protection: the login and signup
 * endpoints are the only ones anybody on the internet can call without a
 * token, and nothing capped how often.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private FilterChain filterChain;

    @InjectMocks private RateLimitFilter rateLimitFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitFilter, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(rateLimitFilter, "maxRequests", 20);
        ReflectionTestUtils.setField(rateLimitFilter, "windowSeconds", 60L);
        ReflectionTestUtils.setField(rateLimitFilter, "enabled", true);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    @DisplayName("a request under the limit is passed on")
    void allowsUnderTheLimit() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(5L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"), response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("15");
    }

    @Test
    @DisplayName("the request that exceeds the limit gets 429 and never reaches the app")
    void rejectsOverTheLimit() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(21L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"), response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    @DisplayName("the limit itself is allowed; only what comes after is refused")
    void theBoundaryIsInclusive() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(20L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"), response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("the window is set once, by the first request of the window")
    void windowIsSetOnFirstRequestOnly() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"),
                new MockHttpServletResponse(), filterChain);

        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("a later request in the same window does not extend it")
    void windowIsNotExtended() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(7L);

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"),
                new MockHttpServletResponse(), filterChain);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("each source is counted separately")
    void countsPerSource() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"),
                new MockHttpServletResponse(), filterChain);
        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.2"),
                new MockHttpServletResponse(), filterChain);

        verify(valueOps).increment("ratelimit:/api/auth/login:10.0.0.1");
        verify(valueOps).increment("ratelimit:/api/auth/login:10.0.0.2");
    }

    @Test
    @DisplayName("behind a proxy the client address is taken from X-Forwarded-For, not the proxy's socket")
    void honoursForwardedFor() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(1L);
        MockHttpServletRequest request = loginRequest("172.17.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 172.17.0.5");

        rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        verify(valueOps).increment("ratelimit:/api/auth/login:203.0.113.9");
    }

    @Test
    @DisplayName("only the unauthenticated endpoints are limited")
    void limitsOnlyTheAuthEndpoints() {
        MockHttpServletRequest patients = new MockHttpServletRequest("GET", "/api/patients");
        patients.setRequestURI("/api/patients");
        MockHttpServletRequest login = loginRequest("10.0.0.1");
        MockHttpServletRequest signup = new MockHttpServletRequest("POST", "/api/auth/signup");
        signup.setRequestURI("/api/auth/signup");

        assertThat(rateLimitFilter.shouldNotFilter(patients)).isTrue();
        assertThat(rateLimitFilter.shouldNotFilter(login)).isFalse();
        assertThat(rateLimitFilter.shouldNotFilter(signup)).isFalse();
    }

    @Test
    @DisplayName("the filter can be switched off entirely")
    void respectsTheOffSwitch() {
        ReflectionTestUtils.setField(rateLimitFilter, "enabled", false);

        assertThat(rateLimitFilter.shouldNotFilter(loginRequest("10.0.0.1"))).isTrue();
    }

    @Test
    @DisplayName("a Redis outage does not take sign-in down with it")
    void degradesOpenWhenRedisIsDown() throws Exception {
        when(valueOps.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(loginRequest("10.0.0.1"), response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
