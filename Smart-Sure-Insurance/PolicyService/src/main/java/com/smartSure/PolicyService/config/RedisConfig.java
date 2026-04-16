package com.smartSure.PolicyService.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {

        // Use a clean ObjectMapper WITHOUT type metadata (@class / enableDefaultTyping).
        // GenericJackson2JsonRedisSerializer embeds "@class" and stores BigDecimal as
        // ["java.math.BigDecimal", 1000.0] — when the service restarts after a code change
        // the stale Redis entries become unreadable, causing circuit breaker failures.
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(om, Object.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)

                .withCacheConfiguration("policyTypes",
                        defaultConfig.entryTtl(Duration.ofMinutes(30))) // reduced from 1hr for fresher data

                .withCacheConfiguration("policyById",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))

                .withCacheConfiguration("customerEmail",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))

                .build();
    }
}