package com.example.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest
class AwsConfigTest {
    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Test
    void dynamoDbClientShouldBeConfigured(){
        assertThat(dynamoDbClient).isNotNull();
        assertThat(dynamoDbClient.serviceName()).isEqualTo("dynamodb");
    }
}