# Scalable API Framework with Spring Boot, Redis, and AWS Lambda

## Project Overview
This project demonstrates a scalable and high-performance REST API built with **Spring Boot**, **AWS Lambda**, **DynamoDB**, and **Redis Pub/Sub**. It handles real-time messaging and processes over 1M+ requests daily with efficient integration of microservices and cloud-based components.

---

## Features
- **Spring Boot Framework**: Simplifies development with robust tools for dependency injection, API handling, and AWS integration.
- **Redis Pub/Sub**: Implements real-time messaging for event notifications and distributed communication.
- **AWS Lambda**: Serverless architecture for cost-effective, scalable compute services.
- **DynamoDB Integration**: Low-latency and highly scalable NoSQL database.
- **Real-Time Notifications**: Enables real-time data updates and notifications using Redis channels.

---

## Prerequisites

### Software Requirements
- **Java 17** or higher
- **Maven** (for dependency management)
- **Redis** (locally or via a cloud provider like AWS ElastiCache)
- **AWS CLI** (for deploying Lambda functions)

### AWS Services Required
- **AWS DynamoDB**: For data storage.
- **AWS Lambda**: For serverless compute.
- **AWS API Gateway**: For managing API endpoints.

---

## Project Setup

### 1. Clone the Repository
```bash
git clone https://github.com/agreatpigeon/scalable-api-framework.git
cd scalable-api-framework
```

### 2. Configure Application Properties
Update `src/main/resources/application.properties` with your Redis and AWS configurations:

```properties
# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=yourpassword

# AWS Configuration
aws.region=your-region
aws.dynamodb.table=your-dynamodb-table
```

### 3. Build the Project
Run the following command to build the project:
```bash
mvn clean package
```

---

## Key Components

### 1. **Redis Publisher**
Located in `RedisPublisherService.java`, this service publishes messages to Redis channels:
```java
@Autowired
private StringRedisTemplate redisTemplate;

public void publishMessage(String channel, String message) {
    redisTemplate.convertAndSend(channel, message);
}
```

### 2. **Redis Subscriber**
Located in `RedisSubscriberService.java`, this service listens for messages from Redis channels:
```java
@Bean
public RedisMessageListenerContainer listenerContainer(RedisMessageListenerAdapter listenerAdapter) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.addMessageListener(listenerAdapter, new ChannelTopic("data-updates"));
    return container;
}
```

### 3. **AWS Lambda Integration**
The application integrates with AWS Lambda using Spring Boot and the `SpringBootRequestHandler` class:
```java
public class LambdaHandler extends SpringBootRequestHandler<Object, Object> {}
```

---

## Deployment

### 1. Package the Application
Build a JAR file suitable for deployment:
```bash
mvn clean package
```

### 2. Deploy to AWS Lambda
1. Create an AWS Lambda function.
2. Upload the JAR file as the Lambda function package.
3. Configure the function handler as:
   ```java
   com.example.api.LambdaHandler
   ```

### 3. Set Up API Gateway
1. Create an API Gateway in AWS.
2. Link it to the Lambda function to expose the REST API.

---

## Testing

### 1. Local Testing
Run the application locally:
```bash
mvn spring-boot:run
```
Test the API using `curl` or tools like Postman:
```bash
curl -X POST http://localhost:8080/api/save \
  -H "Content-Type: application/json" \
  -d '{"id":"123", "payload":"Test data"}'
```

### 2. Redis Messaging
Publish a message:
```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{"channel":"data-updates", "message":"New data added!"}'
```
Observe the subscriber logs for real-time updates.

---

## Future Improvements
- Implement **WebSocket** for real-time client notifications.
- Add **JWT-based authentication** for secure API access.
- Scale Redis with **AWS ElastiCache**.

---

## License
This project is licensed under the MIT License. See `LICENSE` for details.

---

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request.

---

## Contact
For any inquiries or issues, please open an issue in the repository or contact me directly.
