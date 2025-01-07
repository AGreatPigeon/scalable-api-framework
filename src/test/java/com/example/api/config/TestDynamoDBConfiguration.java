package com.example.api.config;

import com.example.api.model.ApiRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

@TestConfiguration
public class TestDynamoDBConfiguration {

    @Bean
    public LocalStackContainer localStackContainer() {
        LocalStackContainer container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                .withServices(LocalStackContainer.Service.DYNAMODB);
        container.start();
        return container;
    }

    @Primary
    @Bean(name = "testDynamoDbClient")
    public DynamoDbClient dynamoDbClient(@Qualifier("localStackContainer") LocalStackContainer localStackContainer) {
        URI endpoint = localStackContainer.getEndpointOverride(LocalStackContainer.Service.DYNAMODB);
        // Provide dummy credentials to LocalStack
        return DynamoDbClient.builder()
                .endpointOverride(endpoint)
                .region(Region.of(localStackContainer.getRegion()))
                .credentialsProvider(() -> {
                    return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("dummy-access-key", "dummy-secret-key");
                })
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<ApiRequest> initializeTable(DynamoDbEnhancedClient dynamoDbEnhancedClient, DynamoDbClient dynamoDbClient, @Value("${dynamodb.table-name}") String tableName) {
        try {
            // Create the table if it doesn't exist
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH) // Partition key
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S) // String type
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build());
        } catch (ResourceInUseException e) {
            // Table already exists
            System.out.println("Table already exists: " + e.getMessage());
        }

        return dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(ApiRequest.class));
    }
}
