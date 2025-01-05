package com.example.api.service;

import com.example.api.Main;
import com.example.api.model.ApiRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(classes = Main.class)
@TestPropertySource(properties = {"aws.region=us-east-1"})
@ActiveProfiles("test")
class ApiServiceTest {

    // Use DockerImageName for LocalStack image
    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.5.0");

    @Container
    public static LocalStackContainer dynamoDbContainer = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withExposedPorts(4566, 8000) // Expose ports required by DynamoDB
            .withEnv("AWS_REGION", "us-east-1");


    @Container
    private final GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    private DynamoDbClient dynamoDbClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ApiService apiService;

    private String redisHost;
    private int redisPort;
    private String dynamoDbEndpoint;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    public void setup() {
        // Get dynamic ports from containers
        redisHost = redisContainer.getHost();
        redisPort = redisContainer.getMappedPort(6379);
        dynamoDbEndpoint = "http://" + dynamoDbContainer.getHost() + ":" + dynamoDbContainer.getMappedPort(8000);

        // Print to confirm region is correctly set
        System.out.println("AWS Region: " + System.getProperty("aws.region"));

        // Set up application properties dynamically
        System.setProperty("spring.redis.host", redisHost);
        System.setProperty("spring.redis.port", String.valueOf(redisPort));
        System.setProperty("dynamodb.endpoint", dynamoDbEndpoint);
        System.setProperty("aws.access-key-id", "test");
        System.setProperty("aws.secret-access-key", "test");
        System.setProperty("aws.region", "us-east-1");

        // Mock the value operations for Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    public void tearDown() {
        // Optionally clear the state or perform any cleanup operations here
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testGetRequest_Cached() {
        String requestId = "123";
        ApiRequest cachedRequest = new ApiRequest();
        cachedRequest.setId(requestId);

        // Simulate Redis call returning a cached object
        when(valueOperations.get(requestId)).thenReturn(cachedRequest);

        ApiRequest result = apiService.getRequest(requestId);

        assertNotNull(result);
        assertEquals(requestId, result.getId());
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(requestId);
        verifyNoInteractions(dynamoDbClient);
    }

    @Test
    void testGetRequest_NotCached() {
        String requestId = "123";
        ApiRequest dbRequest = new ApiRequest();
        dbRequest.setId(requestId);

        // Simulate Redis returning null (cache miss)
        when(valueOperations.get(requestId)).thenReturn(null);

        // Simulate DynamoDB returning an item
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        ApiRequest result = apiService.getRequest(requestId);

        assertNotNull(result);
        assertEquals(requestId, result.getId());
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(requestId);
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
        verify(valueOperations, times(1)).set(requestId, dbRequest);
    }

    @Test
    void testSaveRequest() {
        ApiRequest apiRequest = new ApiRequest();
        apiRequest.setId("123");

        // Mock DynamoDB putItem and Redis set methods
        doNothing().when(dynamoDbClient).putItem(any(PutItemRequest.class));
        doNothing().when(valueOperations).set(apiRequest.getId(), apiRequest);

        apiService.saveRequest(apiRequest);

        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
        verify(valueOperations, times(1)).set(apiRequest.getId(), apiRequest);
    }
}
