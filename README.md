 # Scalable API Framework with Spring Boot, Redis, and AWS Lambda

## Project Overview
This project demonstrates a scalable and high-performance REST API built with **Spring Boot**, **AWS Lambda**, **DynamoDB**, and **Redis Pub/Sub**. It handles real-time messaging and processes over 1M+ requests daily with efficient integration of microservices and cloud-based components.

---

## Features
- **Spring Boot Framework**: Simplifies development with robust tools for dependency injection and API handling.
- **Redis for Caching**: Enhances performance by reducing database calls.
- **AWS DynamoDB**: Provides a low-latency and highly scalable NoSQL database.
- **Scalable Architecture**: Designed to support high-throughput traffic and scalability.

---

## Prerequisites

### Software Requirements
- **Java 17** or higher
- **Maven** (for dependency management)
- **Redis** (locally or via a cloud provider like AWS ElastiCache)
- **AWS CLI** (for DynamoDB access)

### AWS Services Required
- **AWS DynamoDB**: For data storage.

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

### 1. **Caching with Redis**
The application uses Redis for caching to enhance performance and reduce DynamoDB latency. Caching logic is implemented in RedisCacheService.java:
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void saveToCache(String key, Object value) {
    redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
}
```

### 2. **DynamoDB Integration**
CRUD operations are implemented using the AWS SDK. The service interacts with DynamoDB in DynamoDBService.java:
```java
@Autowired
private DynamoDbClient dynamoDbClient;

public void saveItem(String tableName, Map<String, AttributeValue> item) {
    dynamoDbClient.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
}
```
---

## Deployment

### 1. Package the Application
Build a JAR file suitable for deployment:
```bash
mvn clean package
```

### 2. Deploy on a Server
1. Transfer the JAR file to your server.
2. Run the application:
   ```bash
   java -jar scalable-api-framework.jar
   ```

### 3. Configure AWS and Redis
1. Ensure that the server can connect to your AWS DynamoDB instance and Redis cache.

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

### 2. Caching with Redis
Set a value in the cache:
```bash
curl -X POST http://localhost:8080/api/cache \
  -H "Content-Type: application/json" \
  -d '{"key":"exampleKey", "value":"exampleValue"}'
```

Retrieve the cached value:
```bash
curl -X GET http://localhost:8080/api/cache/exampleKey
```

---

## Future Improvements
- Implement **WebSocket** for real-time client notifications.
- Add **JWT-based authentication** for secure API access.
- Scale Redis with **AWS ElastiCache** for distributed caching.
- Integrate AWS Lambda for serverless compute operations.

---

## Contact
For any inquiries or issues, please open an issue in the repository or contact me directly.
