package com.rahul.ticketbooking.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.rahul.ticketbooking.dto.")
                .allowIfSubType("java.util.")
                .allowIfSubType("org.springframework.cache.support.")   // for the "empty result" marker
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport()   // lets Redis store "this does not exist"
                .build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl((key, value) -> calculateTtl(value))   // TTL is decided per entry
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    private Duration calculateTtl(Object value) {
        // Empty result ("movie does not exist"): keep it only for 1 minute
        if (value == null || value instanceof NullValue) {
            return Duration.ofMinutes(1);
        }
        // Normal entry: 10 minutes plus a random 0 to 120 seconds, so entries do not expire together
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 121);
        return Duration.ofMinutes(10).plusSeconds(jitterSeconds);
    }
}