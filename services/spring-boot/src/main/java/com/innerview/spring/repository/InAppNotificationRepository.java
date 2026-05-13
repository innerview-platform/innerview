package com.innerview.spring.repository;

import com.innerview.spring.entity.InAppNotification;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InAppNotificationRepository {

  private static final long TTL_SECONDS = 30L * 24 * 60 * 60; // 30-day inbox window

  private final DynamoDbClient dynamo;

  @Value("${notification.dynamo.inapp-table}")
  private String tableName;

  // -------------------------------------------------------------------------
  // Write
  // -------------------------------------------------------------------------

  public void save(InAppNotification n) {
    String sk = sortKey(n.getCreatedAt(), n.getEventId());

    dynamo.putItem(
        PutItemRequest.builder()
            .tableName(tableName)
            .item(
                Map.of(
                    "recipientId", attr(n.getRecipientId().toString()),
                    "sk", attr(sk),
                    "eventId", attr(n.getEventId()),
                    "payload", attr(n.getPayload()),
                    "createdAt", numAttr(n.getCreatedAt()),
                    "ttlEpochSec", numAttr(n.getTtlEpochSec()),
                    "read", boolAttr(n.isRead())))
            // Idempotent — safe to call twice for the same eventId
            .conditionExpression("attribute_not_exists(recipientId)")
            .build());

    log.debug(
        "Saved InAppNotification eventId={} recipientId={}", n.getEventId(), n.getRecipientId());
  }

  // -------------------------------------------------------------------------
  // Read — REST endpoint (last 30 days)
  // -------------------------------------------------------------------------

  public List<InAppNotification> findLast30Days(UUID recipientId) {
    long windowMs = Instant.now().toEpochMilli() - 30L * 24 * 60 * 60 * 1000;
    String skStart = sortKey(windowMs, ""); // "" sorts before any UUID

    QueryResponse response =
        dynamo.query(
            QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("recipientId = :rid AND sk >= :skStart")
                .expressionAttributeValues(
                    Map.of(
                        ":rid", attr(recipientId.toString()),
                        ":skStart", attr(skStart)))
                // Newest first: DynamoDB returns ascending by default; scan backward
                .scanIndexForward(false)
                .build());

    return response.items().stream().map(this::fromItem).collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Update — mark as read
  // -------------------------------------------------------------------------

  public void markRead(String eventId, UUID recipientId, long createdAt) {
    String sk = sortKey(createdAt, eventId);

    dynamo.updateItem(
        UpdateItemRequest.builder()
            .tableName(tableName)
            .key(
                Map.of(
                    "recipientId", attr(recipientId.toString()),
                    "sk", attr(sk)))
            .updateExpression("SET #r = :true")
            .expressionAttributeNames(Map.of("#r", "read"))
            .expressionAttributeValues(Map.of(":true", boolAttr(true)))
            .build());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static String sortKey(long epochMs, String eventId) {
    return String.format("%013d#%s", epochMs, eventId);
  }

  private InAppNotification fromItem(Map<String, AttributeValue> item) {
    return InAppNotification.builder()
        .recipientId(UUID.fromString(item.get("recipientId").s()))
        .eventId(item.get("eventId").s())
        .payload(item.get("payload").s())
        .createdAt(Long.parseLong(item.get("createdAt").n()))
        .ttlEpochSec(Long.parseLong(item.get("ttlEpochSec").n()))
        .read(item.get("read").bool())
        .build();
  }

  private static AttributeValue attr(String s) {
    return AttributeValue.fromS(s);
  }

  private static AttributeValue numAttr(long n) {
    return AttributeValue.fromN(String.valueOf(n));
  }

  private static AttributeValue boolAttr(boolean b) {
    return AttributeValue.fromBool(b);
  }
}
