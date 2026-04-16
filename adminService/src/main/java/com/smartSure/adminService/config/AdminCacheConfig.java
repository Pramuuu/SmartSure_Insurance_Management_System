package com.smartSure.adminService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

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

    /** Clean ObjectMapper — NO @class type embedding to prevent stale-cache crashes */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Initializing Redis-based distributed cache manager for AdminService");

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(buildObjectMapper().activateDefaultTyping(
                buildObjectMapper().getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        ));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(AUDIT_LOGS_CACHE,        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(USER_MANAGEMENT_CACHE,   defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(SYSTEM_STATISTICS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration(DASHBOARD_DATA_CACHE,    defaultConfig.entryTtl(Duration.ofMinutes(3)))
                .withCacheConfiguration(RECENT_ACTIVITY_CACHE,   defaultConfig.entryTtl(Duration.ofMinutes(1)))
                .withCacheConfiguration(ADMIN_REPORTS_CACHE,     defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .build();

        log.info("Redis cache manager initialized with 6 cache regions for AdminService");
        return cacheManager;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(buildObjectMapper().activateDefaultTyping(
                buildObjectMapper().getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        ));
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}