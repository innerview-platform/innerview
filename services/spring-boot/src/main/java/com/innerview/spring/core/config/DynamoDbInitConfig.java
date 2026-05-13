package com.innerview.spring.core.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Configuration
public class DynamoDbInitConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbInitConfig.class);
    private final DynamoDbClient dynamoDbClient;

    public DynamoDbInitConfig(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @PostConstruct
    public void createTable() {
        // Run in background — never block Spring startup
        Thread.ofVirtual().name("dynamo-init").start(() -> {
            String tableName = "interview-notification-outbox";
            int maxRetries = 5;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    dynamoDbClient.describeTable(DescribeTableRequest.builder()
                            .tableName(tableName)
                            .build());
                    log.info("Table {} already exists.", tableName);
                    return;

                } catch (ResourceNotFoundException e) {
                    log.info("Table {} does not exist. Creating...", tableName);
                    dynamoDbClient.createTable(CreateTableRequest.builder()
                            .tableName(tableName)
                            .attributeDefinitions(
                                    AttributeDefinition.builder().attributeName("eventId").attributeType(ScalarAttributeType.S).build(),
                                    AttributeDefinition.builder().attributeName("channel").attributeType(ScalarAttributeType.S).build()
                            )
                            .keySchema(
                                    KeySchemaElement.builder().attributeName("eventId").keyType(KeyType.HASH).build(),
                                    KeySchemaElement.builder().attributeName("channel").keyType(KeyType.RANGE).build()
                            )
                            .billingMode(BillingMode.PAY_PER_REQUEST)
                            .build());
                    log.info("Table {} created successfully.", tableName);
                    return;

                } catch (Exception e) {
                    log.warn("DynamoDB init attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(3000L * attempt); // back off: 3s, 6s, 9s...
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            log.error("DynamoDB table init failed after {} attempts — app running without it.", maxRetries);
        });
    }
}