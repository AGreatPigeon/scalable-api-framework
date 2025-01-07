package com.example.api.service;

import com.example.api.model.ApiRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@PropertySource("classpath:application.properties")
public class KafkaConsumerService {

    @Value("${kafka.topic}")
    private String topic;

    @Value("${kafka.apigroup}")
    private String group;

    @Autowired
    private ApiService apiService;

    @KafkaListener(topics = "api-requests", groupId = "api-group")
    public void consume(ConsumerRecord<String, String> record) {
        System.out.println("Consumed message: Key = " + record.key() + ", Value = " + record.value());
        // Process the message (e.g., log, trigger downstream actions)
        ApiRequest request = new ApiRequest();
        request.setId(record.key());
        request.setPayload(record.value());
        apiService.saveRequest(request); // Persist to DynamoDB
        System.out.println("Message stored in database.");
    }
}
