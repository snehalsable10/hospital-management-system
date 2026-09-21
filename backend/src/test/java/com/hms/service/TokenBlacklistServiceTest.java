package com.hms.service;

import com.hms.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Revoked tokens used to live in a plain map, which meant a restart
 * un-revoked every token ever logged out, a second instance knew nothing of
 * the first one's logouts, and the method that bounded the map's growth was
 * never called by anything.
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.payload.signature";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("a revoked token is stored as a digest, never verbatim")
    void storesADigestNotTheToken() {
        when(jwtTokenProvider.getExpiry(TOKEN))
                .thenReturn(Optional.of(Instant.now().plusSeconds(3600)));

        tokenBlacklistService.blacklistToken(TOKEN);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(key.capture(), anyString(), any(Duration.class));

        // A token is a live credential until it expires; a dump of Redis
        // should not hand anyone a set of working ones.
        assertThat(key.getValue()).doesNotContain(TOKEN);
        assertThat(key.getValue()).startsWith("jwt:revoked:");
        assertThat(key.getValue()).hasSize("jwt:revoked:".length() + 64);  // SHA-256 hex
    }

    @Test
    @DisplayName("the entry expires when the token would have expired anyway")
    void ttlIsTheTokensRemainingLife() {
        when(jwtTokenProvider.getExpiry(TOKEN))
                .thenReturn(Optional.of(Instant.now().plusSeconds(1800)));

        tokenBlacklistService.blacklistToken(TOKEN);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(anyString(), anyString(), ttl.capture());

        assertThat(ttl.getValue().toSeconds()).isBetween(1740L, 1800L);
    }

    @Test
    @DisplayName("an already-expired token is not stored - the signature check rejects it anyway")
    void expiredTokenIsNotStored() {
        when(jwtTokenProvider.getExpiry(TOKEN))
                .thenReturn(Optional.of(Instant.now().minusSeconds(60)));

        tokenBlacklistService.blacklistToken(TOKEN);

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("a revoked token reads back as revoked")
    void revokedTokenIsRecognised() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThat(tokenBlacklistService.isTokenBlacklisted(TOKEN)).isTrue();
    }

    @Test
    @DisplayName("a token that was never revoked is not")
    void unknownTokenIsNotRevoked() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertThat(tokenBlacklistService.isTokenBlacklisted(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("the same token always maps to the same key, so a logout is findable")
    void digestIsStable() {
        when(jwtTokenProvider.getExpiry(TOKEN))
                .thenReturn(Optional.of(Instant.now().plusSeconds(3600)));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        tokenBlacklistService.blacklistToken(TOKEN);
        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(written.capture(), anyString(), any(Duration.class));

        tokenBlacklistService.isTokenBlacklisted(TOKEN);
        ArgumentCaptor<String> read = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).hasKey(read.capture());

        assertThat(read.getValue()).isEqualTo(written.getValue());
    }

    @Test
    @DisplayName("different tokens map to different keys")
    void differentTokensDifferentKeys() {
        when(jwtTokenProvider.getExpiry(anyString()))
                .thenReturn(Optional.of(Instant.now().plusSeconds(3600)));

        tokenBlacklistService.blacklistToken(TOKEN);
        tokenBlacklistService.blacklistToken(TOKEN + "x");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(valueOps, org.mockito.Mockito.times(2))
                .set(keys.capture(), anyString(), any(Duration.class));

        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("with Redis down the revocation is kept in memory rather than dropped")
    void fallsBackToMemoryOnWrite() {
        when(jwtTokenProvider.getExpiry(TOKEN))
                .thenReturn(Optional.of(Instant.now().plusSeconds(3600)));
        doThrow(new RedisConnectionFailureException("Unable to connect to Redis"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        tokenBlacklistService.blacklistToken(TOKEN);

        assertThat(tokenBlacklistService.getFallbackSize()).isEqualTo(1);
        assertThat(tokenBlacklistService.fallbackExpiryOf(TOKEN)).isPresent();
    }

    @Test
    @DisplayName("with Redis down the in-memory list is what answers, so a logout still holds")
    void readsFromTheFallbackOnOutage() {
        when(jwtTokenProvider.getExpiry(TOKEN))
                .thenReturn(Optional.of(Instant.now().plusSeconds(3600)));
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));

        tokenBlacklistService.blacklistToken(TOKEN);

        assertThat(tokenBlacklistService.isTokenBlacklisted(TOKEN)).isTrue();
        assertThat(tokenBlacklistService.isTokenBlacklisted("some.other.token")).isFalse();
    }

    @Test
    @DisplayName("a null or blank token is ignored rather than stored")
    void ignoresEmptyInput() {
        tokenBlacklistService.blacklistToken(null);
        tokenBlacklistService.blacklistToken("   ");

        assertThat(tokenBlacklistService.isTokenBlacklisted(null)).isFalse();
        assertThat(tokenBlacklistService.isTokenBlacklisted("")).isFalse();
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("an unreadable token is still held, for the configured token lifetime")
    void unreadableTokenStillRevoked() {
        when(jwtTokenProvider.getExpiry(TOKEN)).thenReturn(Optional.empty());

        tokenBlacklistService.blacklistToken(TOKEN);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(anyString(), anyString(), ttl.capture());
        assertThat(ttl.getValue()).isEqualTo(Duration.ofDays(1));
    }
}
