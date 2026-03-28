package com.smartSure.paymentService.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
 * Distributed Caching Configuration for PaymentService
 * 
 * This configuration class sets up Redis-based distributed caching to improve
 * performance by caching frequently accessed payment data such as payment
 * transactions, Razorpay order details, customer payment history, and
 * payment statistics across multiple service instances.
 * 
 * Key Features:
 * - Redis as the distributed cache store
 * - JSON serialization for complex objects
 * - Configurable TTL for different cache regions
 * - Connection pooling for optimal performance
 * - Cache eviction strategies for data consistency
 * - Payment gateway integration optimization
 * 
 * @author SmartSure Development Team
 * @version 1.0
 * @since 2024-03-25
 */
@Slf4j
@Configuration
@EnableCaching
public class PaymentCacheConfig {

    /**
     * Cache region names used throughout the PaymentService
     */
    public static final String PAYMENTS_CACHE = "payments";
    public static final String RAZORPAY_ORDERS_CACHE = "razorpay-orders";
    public static final String CUSTOMER_PAYMENTS_CACHE = "customer-payments";
    public static final String POLICY_PAYMENTS_CACHE = "policy-payments";
    public static final String PAYMENT_STATISTICS_CACHE = "payment-statistics";
    public static final String FAILED_PAYMENTS_CACHE = "failed-payments";

    /**
     * Configures the Redis-based cache manager with custom serialization
     * and TTL settings for different cache regions.
     * 
     * @param redisConnectionFactory Redis connection factory
     * @return Configured RedisCacheManager instance
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Initializing Redis-based distributed cache manager for PaymentService");
        
        // Configure JSON serialization for cache values
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                                         ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default 10 minutes TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        // Build cache manager with specific configurations for different regions
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                // Individual payments - medium TTL as payment status may change
                .withCacheConfiguration(PAYMENTS_CACHE, 
                    defaultConfig.entryTtl(Duration.ofMinutes(15)))
                // Razorpay orders - short TTL as they expire quickly
                .withCacheConfiguration(RAZORPAY_ORDERS_CACHE, 
                    defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // Customer payment history - longer TTL as history doesn't change
                .withCacheConfiguration(CUSTOMER_PAYMENTS_CACHE, 
                    defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // Policy payments - medium TTL
                .withCacheConfiguration(POLICY_PAYMENTS_CACHE, 
                    defaultConfig.entryTtl(Duration.ofMinutes(20)))
                // Payment statistics - short TTL for real-time data
                .withCacheConfiguration(PAYMENT_STATISTICS_CACHE, 
                    defaultConfig.entryTtl(Duration.ofMinutes(3)))
                // Failed payments - medium TTL for retry analysis
                .withCacheConfiguration(FAILED_PAYMENTS_CACHE, 
                    defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .build();

        log.info("Redis cache manager initialized with {} cache regions for PaymentService", 6);
        return cacheManager;
    }

    /**
     * Configures RedisTemplate for manual cache operations and complex queries.
     * 
     * @param redisConnectionFactory Redis connection factory
     * @return Configured RedisTemplate instance
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.debug("Configuring RedisTemplate for manual cache operations in PaymentService");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Configure serializers
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                                         ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // Set serializers
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        
        log.debug("RedisTemplate configured successfully for PaymentService");
        return template;
    }
}