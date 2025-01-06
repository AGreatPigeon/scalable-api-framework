package com.example.api.config;

import com.example.api.model.ApiRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@TestConfiguration
public class TestRedisConfiguration {

    // Create and start the Redis container using Testcontainers
    @Bean
    @Qualifier("redisContainer")
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:latest"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
        container.start();
        return container;
    }

    // Create a RedisConnectionFactory that connects to the Testcontainers Redis instance
    @Primary
    @Bean(name = "testRedisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory(@Qualifier("redisContainer") GenericContainer<?> redisContainer) {
        String host = redisContainer.getHost();
        Integer port = redisContainer.getMappedPort(6379);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet(); // Ensure the factory is initialized

        System.setProperty("spring.redis.host", host);
        System.setProperty("spring.redis.port", port.toString());

        return factory;
    }

    // Configure RedisTemplate with Jackson2JsonRedisSerializer to serialize ApiRequest objects
    @Primary
    @Bean(name = "testRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Use StringRedisSerializer for keys (String keys)
        template.setKeySerializer(new StringRedisSerializer());

        // Use Jackson2JsonRedisSerializer to serialize/deserialize ApiRequest objects to/from JSON
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(ApiRequest.class));

        return template;
    }

    // Create ValueOperations to interact with Redis
    @Primary
    @Bean(name = "testValueOperations")
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }
}

