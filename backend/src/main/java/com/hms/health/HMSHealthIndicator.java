package com.hms.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Slf4j
@Component
@RequiredArgsConstructor
public class HMSHealthIndicator implements HealthIndicator {

    private final EntityManager entityManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // Check database connectivity
            Health dbHealth = checkDatabaseHealth();
            if (dbHealth.getStatus().getCode().equals("DOWN")) {
                return dbHealth;
            }

            // Check Redis connectivity
            Health redisHealth = checkRedisHealth();
            if (redisHealth.getStatus().getCode().equals("DOWN")) {
                return redisHealth;
            }

            // All systems operational
            return Health.up()
                    .withDetail("database", "Connected")
                    .withDetail("redis", "Connected")
                    .withDetail("status", "All systems operational")
                    .build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Health checkDatabaseHealth() {
        try {
            Query query = entityManager.createNativeQuery("SELECT 1");
            query.getSingleResult();

            return Health.up()
                    .withDetail("database", "PostgreSQL connected")
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("database", "PostgreSQL connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Health checkRedisHealth() {
        try {
            // Test Redis connection with PING
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "Cache connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Cache ping failed")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Redis health check failed (optional dependency)", e);
            // Redis is optional, so return UP with warning
            return Health.up()
                    .withDetail("redis", "Optional - not available")
                    .build();
        }
    }
}
