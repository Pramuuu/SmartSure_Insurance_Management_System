package com.smartSure.adminService.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Distributed Caching Configuration for AdminService
 */
@Slf4j
@Configuration
@EnableCaching
public class AdminCacheConfig {

    public static final String AUDIT_LOGS_CACHE = "audit-logs";
    public static final String USER_MANAGEMENT_CACHE = "user-management";
    public static final String SYSTEM_STATISTICS_CACHE = "system-statistics";
    public static final String DASHBOARD_DATA_CACHE = "dashboard-data";
    public static final String RECENT_ACTIVITY_CACHE = "recent-activity";
    public static final String ADMIN_REPORTS_CACHE = "admin-reports";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Initializing Redis-based distributed cache manager for AdminService");

        // Configure Jackson serializer
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTypingAsProperty(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                "@class"
        );

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        // Build cache manager with region-specific TTLs
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(AUDIT_LOGS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(USER_MANAGEMENT_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(SYSTEM_STATISTICS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration(DASHBOARD_DATA_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(3)))
                .withCacheConfiguration(RECENT_ACTIVITY_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(1)))
                .withCacheConfiguration(ADMIN_REPORTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .build();

        log.info("Redis cache manager initialized with {} cache regions for AdminService", 6);
        return cacheManager;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.debug("Configuring RedisTemplate for manual cache operations in AdminService");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configure Jackson serializer
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTypingAsProperty(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                "@class"
        );

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Set serializers
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();

        log.debug("RedisTemplate configured successfully for AdminService");
        return template;
    }
}