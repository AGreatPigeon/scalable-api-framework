package com.example.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import com.example.api.model.ApiRequest;

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

    @Value("${kafka.topic}")
    private String topic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ApiService(DynamoDbClient dynamoDbClient, KafkaTemplate<String, String> kafkaTemplate){
        this.dynamoDbClient = dynamoDbClient;
        this.kafkaTemplate = kafkaTemplate;
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
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        DynamoDbTable<ApiRequest> mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(ApiRequest.class));
        try{
            ApiRequest response = mappedTable.getItem(r -> r.key(k -> k.partitionValue(requestId)));
            if (response == null){
                // Log that the item wasn't found
                System.err.println("No item found in DynamoDB for ID: " + requestId);
                return null;
            }
            return response;
        } catch (Exception e){
            // Log the exception and rethrow or handle it
            System.err.println("Error fetching item from DynamoDB: " + e.getMessage());
            throw e;
        }
    }

    public void saveRequest(ApiRequest apiRequest) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        DynamoDbTable<ApiRequest> mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(ApiRequest.class));
        mappedTable.putItem(apiRequest);

        kafkaTemplate.send(topic, apiRequest.getId(), apiRequest.getPayload());
        redisTemplate.opsForValue().set(apiRequest.getId(), apiRequest);
    }
}
