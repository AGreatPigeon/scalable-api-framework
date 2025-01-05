package com.example.api.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import com.example.api.model.ApiRequest;

import java.net.URI;

@Service
@PropertySource("classpath:application.properties")
public class ApiService {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Value("${dynamodb.endpoint}")
    private String dynamoDbEndpoint;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${dynamodb.table-name}")
    private String tableName;

    @Value("${aws.region}")
    private String awsRegion;

    private final DynamoDbEnhancedClient enhancedClient;

    public ApiService(DynamoDbClient dynamoDbClient){
        this.dynamoDbClient = DynamoDbClient.builder().
                region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:8000"))
                .build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    public ApiRequest getRequest(String requestId){
        ApiRequest cachedRequest = (ApiRequest) redisTemplate.opsForValue().get(requestId);
        if (cachedRequest != null){
            return cachedRequest;
        }

        ApiRequest apiRequest = fetchFromDynamoDb(requestId);
        if (apiRequest != null){
            redisTemplate.opsForValue().set(requestId, apiRequest);
        }

        return apiRequest;
    }

    private ApiRequest fetchFromDynamoDb(String requestId){
        DynamoDbTable<ApiRequest> mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(ApiRequest.class));
        return mappedTable.getItem(r -> r.key(k -> k.partitionValue(requestId)));
    }

    public void saveRequest(ApiRequest apiRequest) {
        DynamoDbTable<ApiRequest> mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(ApiRequest.class));
        mappedTable.putItem(apiRequest);

        redisTemplate.opsForValue().set(apiRequest.getId(), apiRequest);
    }

    // Ensure DynamoDbClient is properly configured with endpoint and region
    @PostConstruct
    public void setupDynamoDbClient() {
        // Use endpoint and region from configuration properties
        this.dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDbEndpoint))
                .region(Region.of(awsRegion))  // Dynamically set region from properties
                .build();
    }
}
