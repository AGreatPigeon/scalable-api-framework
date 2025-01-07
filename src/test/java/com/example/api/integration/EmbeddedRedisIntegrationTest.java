package com.example.api.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import redis.embedded.RedisServer;

@SpringBootTest
class EmbeddedRedisIntegrationTest {

    private static RedisServer redisServer;

    @BeforeAll
    static void startRedis() throws Exception {
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        redisServer.stop();
    }
}
