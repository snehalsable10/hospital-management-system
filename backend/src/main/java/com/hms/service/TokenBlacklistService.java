package com.hms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, LocalDateTime> blacklist = new ConcurrentHashMap<>();

    /**
     * Add token to blacklist (on logout)
     */
    public void blacklistToken(String token) {
        blacklist.put(token, LocalDateTime.now());
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Remove expired blacklist entries (older than 24 hours)
     * Call this periodically to cleanup
     */
    public void cleanupExpiredEntries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoffTime));
    }

    /**
     * Clear all blacklist entries (for testing)
     */
    public void clearBlacklist() {
        blacklist.clear();
    }

    /**
     * Get blacklist size
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }
}