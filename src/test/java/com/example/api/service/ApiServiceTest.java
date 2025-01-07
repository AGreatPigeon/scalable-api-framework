package com.example.api.service;

import com.example.api.config.TestDynamoDBConfiguration;
import com.example.api.config.TestRedisConfiguration;
import com.example.api.model.ApiRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Import({TestRedisConfiguration.class, TestDynamoDBConfiguration.class})
class ApiServiceTest {

    @Autowired
    @Qualifier("testDynamoDbClient")
    private DynamoDbClient dynamoDbClient;

    @Autowired
    @Qualifier("testRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ApiService apiService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TEST_REQUEST_ID = "123";
    private static final String TEST_PAYLOAD = "Test Request";

    private ApiRequest apiRequest;

    @BeforeEach
    public void setup() {

        // Initialize test data
        apiRequest = new ApiRequest();
        apiRequest.setId(TEST_REQUEST_ID);
        apiRequest.setPayload(TEST_PAYLOAD);

        apiService.saveRequest(apiRequest);
    }

    @Test
    void testGetRequest_Cached() {
        // Cache the object
        ApiRequest cachedRequest = new ApiRequest();
        cachedRequest.setId(TEST_REQUEST_ID);
        cachedRequest.setPayload(TEST_PAYLOAD);

        // Cache the request in Redis
        redisTemplate.opsForValue().set(TEST_REQUEST_ID, cachedRequest);

        // Fetch from cache
        ApiRequest result = apiService.getRequest(TEST_REQUEST_ID);

        // Assert that the payload matches the cached request
        assertEquals(TEST_PAYLOAD, result.getPayload());
    }

    @Test
    void testGetRequest_NotCached() {
        // Ensure cache is empty by deleting the key from Redis
        redisTemplate.delete(TEST_REQUEST_ID);

        // Mock DynamoDB's GetItemResponse to return the item
        apiService.saveRequest(apiRequest);  // Ensure the request is saved in DynamoDB before fetching

        // Fetch from DynamoDB (which will then be cached in Redis)
        ApiRequest result = apiService.getRequest(TEST_REQUEST_ID);

        // Assert that the payload matches the expected value
        assertEquals(TEST_PAYLOAD, result.getPayload());
    }

    @Test
    void testSaveRequest() {
        // Save request to DynamoDB and Redis
        apiService.saveRequest(apiRequest);

        // Check DynamoDB to see if the item is saved
        ApiRequest savedInDynamoDb = apiService.getRequest(TEST_REQUEST_ID); // Should fetch from DynamoDB
        assertNotNull(savedInDynamoDb, "The request should be saved in DynamoDB.");
        assertEquals(TEST_PAYLOAD, savedInDynamoDb.getPayload(), "The payload should match the saved request.");

        // Check Redis to ensure the cache is populated
        ApiRequest savedInCache = (ApiRequest) redisTemplate.opsForValue().get(TEST_REQUEST_ID);
        assertNotNull(savedInCache, "The request should be cached in Redis.");
        assertEquals(TEST_PAYLOAD, savedInCache.getPayload(), "The cached payload should match the saved request.");
    }

    @Test
    void testKafkaProducer() {
        kafkaTemplate.send("api-requests", "123", "Test Payload");
        // Verify using a mock Kafka consumer or inspect logs
    }

}

