package com.hms.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {

    /**
     * Redis connection factory using Lettuce
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    /**
     * Configure cache manager with TTL settings.
     * Different cache types have different expiration times.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));  // Default TTL: 10 minutes

        // Patient cache: 15 minutes
        RedisCacheConfiguration patientConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15));

        // Doctor cache: 20 minutes
        RedisCacheConfiguration doctorConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20));

        // Department cache: 30 minutes (rarely changes)
        RedisCacheConfiguration departmentConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30));

        // Appointment cache: 5 minutes (frequently changes)
        RedisCacheConfiguration appointmentConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5));

        return RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(config)
                .withCacheConfiguration("patients", patientConfig)
                .withCacheConfiguration("patient", patientConfig)
                .withCacheConfiguration("doctors", doctorConfig)
                .withCacheConfiguration("doctor", doctorConfig)
                .withCacheConfiguration("departments", departmentConfig)
                .withCacheConfiguration("department", departmentConfig)
                .withCacheConfiguration("appointments", appointmentConfig)
                .withCacheConfiguration("appointment", appointmentConfig)
                .build();
    }

    /**
     * When Redis is unreachable, log it and let the request fall through to the
     * database instead of failing. A cache is an optimization; losing it should
     * cost performance, not availability. Registered via CachingConfigurer so
     * Spring's cache AOP proxy uses it for every @Cacheable/@CacheEvict call.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis GET failed (cache={}, key={}): {} — falling through to the database",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed (cache={}, key={}): {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis EVICT failed (cache={}, key={}): {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis CLEAR failed (cache={}): {}", cache.getName(), exception.getMessage());
            }
        };
    }

    /**
     * Configure RedisTemplate for custom Redis operations
     * Serializes objects to JSON format
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create Jackson serializer
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(mapper);

        // String serializer
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Set key-value serialization
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);

        // Set hash key-value serialization
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}