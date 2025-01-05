package com.example.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${spring.redis.timeout}")
    private int redisTimeout;

    @Value("${spring.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        if (!sentinelMaster.isEmpty() && !sentinelNodes.isEmpty()) {
            // Configure Redis Sentinel
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                    .master(sentinelMaster);

            // Add sentinel nodes
            for (String node : sentinelNodes.split(",")) {
                String[] parts = node.split(":");
                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
            }

            return new LettuceConnectionFactory(sentinelConfig, LettucePoolingClientConfiguration.defaultConfiguration());
        } else {
            // Configure Standalone Redis
            return new LettuceConnectionFactory(redisHost, redisPort);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
