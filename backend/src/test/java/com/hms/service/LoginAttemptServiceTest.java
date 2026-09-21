package com.hms.service;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Nothing used to stand between an attacker and unlimited password guesses
 * against a known email address.
 */
@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final String EMAIL = "victim@hms.local";
    private static final String KEY = "auth:failures:victim@hms.local";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockoutMinutes", 15L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("an account below the limit is not locked")
    void notLockedBelowTheLimit() {
        when(valueOps.get(KEY)).thenReturn("4");

        assertThat(loginAttemptService.isLocked(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("an account is locked once it reaches the limit")
    void lockedAtTheLimit() {
        when(valueOps.get(KEY)).thenReturn("5");

        assertThat(loginAttemptService.isLocked(EMAIL)).isTrue();
    }

    @Test
    @DisplayName("an account stays locked past the limit")
    void stillLockedBeyondTheLimit() {
        when(valueOps.get(KEY)).thenReturn("99");

        assertThat(loginAttemptService.isLocked(EMAIL)).isTrue();
    }

    @Test
    @DisplayName("an account with no failures on record is not locked")
    void notLockedWithNoRecord() {
        when(valueOps.get(KEY)).thenReturn(null);

        assertThat(loginAttemptService.isLocked(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("every failure pushes the expiry out, so guesses cannot be paced under the window")
    void theWindowSlidesOnEachFailure() {
        when(valueOps.increment(KEY)).thenReturn(3L);

        assertThat(loginAttemptService.recordFailure(EMAIL)).isEqualTo(3L);

        verify(valueOps).increment(KEY);
        verify(redisTemplate).expire(KEY, Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("a successful sign-in clears the count")
    void successClearsTheCount() {
        loginAttemptService.reset(EMAIL);

        verify(redisTemplate).delete(KEY);
    }

    @Test
    @DisplayName("the email is matched case- and whitespace-insensitively, so padding cannot dodge the lock")
    void keyIsNormalised() {
        when(valueOps.get(KEY)).thenReturn("5");

        assertThat(loginAttemptService.isLocked("  VICTIM@HMS.Local  ")).isTrue();
    }

    @Test
    @DisplayName("a Redis outage does not lock everybody out of the application")
    void degradesOpenWhenRedisIsDown() {
        when(valueOps.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

        assertThat(loginAttemptService.isLocked(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("a Redis outage during a failed attempt does not throw into the login flow")
    void recordingSurvivesARedisOutage() {
        when(valueOps.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

        assertThat(loginAttemptService.recordFailure(EMAIL)).isZero();
    }

    @Test
    @DisplayName("a blank or missing email is never treated as a locked account")
    void blankEmailIsNotLocked() {
        assertThat(loginAttemptService.isLocked(null)).isFalse();
        assertThat(loginAttemptService.isLocked("  ")).isFalse();
        assertThat(loginAttemptService.recordFailure(null)).isZero();

        verify(redisTemplate, org.mockito.Mockito.never()).expire(any(), any());
    }

    @Test
    @DisplayName("a corrupt counter is treated as unlocked rather than throwing")
    void corruptCounterDoesNotThrow() {
        when(valueOps.get(KEY)).thenReturn("not-a-number");

        assertThat(loginAttemptService.isLocked(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("the configured limit and window are what the login message reports")
    void exposesItsConfiguration() {
        assertThat(loginAttemptService.getMaxAttempts()).isEqualTo(5);
        assertThat(loginAttemptService.getLockoutMinutes()).isEqualTo(15L);
    }

    @Test
    @DisplayName("two different accounts are counted separately")
    void countsPerAccount() {
        when(valueOps.get(KEY)).thenReturn("5");
        when(valueOps.get("auth:failures:someone.else@hms.local")).thenReturn("1");

        assertThat(loginAttemptService.isLocked(EMAIL)).isTrue();
        assertThat(loginAttemptService.isLocked("someone.else@hms.local")).isFalse();
    }

    @Test
    @DisplayName("the counter key never contains the password or any credential")
    void keyCarriesOnlyTheEmail() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        loginAttemptService.recordFailure(EMAIL);

        verify(valueOps).increment(eq(KEY));
    }
}
