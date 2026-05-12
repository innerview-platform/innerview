package com.innerview.spring.repository;

import com.innerview.spring.entity.OutboxRecord;
import com.innerview.spring.enums.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DynamoDB persistence layer for outbox records.
 *
 * Table:      interview-notification-outbox
 * PK:         eventId  (String)
 * SK:         channel  (String) — "inApp" or "email"
 */
public class OutboxRepository {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepository.class);

    static final String TABLE_NAME    = "interview-notification-outbox";
    static final String ATTR_EVENT_ID = "eventId";
    static final String ATTR_CHANNEL  = "channel";
    static final String ATTR_STATUS   = "status";
    static final String ATTR_ATTEMPTS = "attempts";
    static final String ATTR_NEXT_RETRY    = "nextRetry";
    static final String ATTR_SENDING_AT    = "sendingAt";
    static final String ATTR_SES_MSG_ID    = "sesMessageId";
    static final String ATTR_CREATED_AT    = "createdAt";

    private final DynamoDbClient dynamo;

    public OutboxRepository(DynamoDbClient dynamo) {
        this.dynamo = dynamo;
    }

    //  Key helpers

    private Map<String, AttributeValue> key(String eventId, String channel) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(ATTR_EVENT_ID, s(eventId));
        key.put(ATTR_CHANNEL,  s(channel));
        return key;
    }

    //  Initial write

    public boolean putPending(OutboxRecord record) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(ATTR_EVENT_ID,  s(record.getEventId()));
        item.put(ATTR_CHANNEL,   s(record.getChannel()));

        // Use .name() to convert Enum to String for DynamoDB
        item.put(ATTR_STATUS,    s(OutboxStatus.PENDING.name()));

        item.put(ATTR_ATTEMPTS,  n(record.getAttempts()));
        item.put(ATTR_NEXT_RETRY, n(record.getNextRetry()));
        item.put(ATTR_CREATED_AT, n(record.getCreatedAt()));

        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build());
            log.debug("Outbox record written: eventId={} channel={}", record.getEventId(), record.getChannel());
            return true;
        } catch (DynamoDbException e) {
            log.error("Failed to write outbox record: eventId={} channel={} error={}",
                    record.getEventId(), record.getChannel(), e.getMessage(), e);
            return false;
        }
    }

    // PENDING → SENDING (conditional lock)

    public boolean acquireSendingLock(String eventId, String channel, long sendingAtEpochMs) {
        try {
            dynamo.updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key(eventId, channel))
                    .updateExpression("SET #s = :sending, sendingAt = :sendingAt")
                    .conditionExpression("#s = :pending")
                    .expressionAttributeNames(Map.of("#s", ATTR_STATUS))
                    .expressionAttributeValues(Map.of(
                            // Convert Enums to Strings
                            ":sending",   s(OutboxStatus.SENDING.name()),
                            ":pending",   s(OutboxStatus.PENDING.name()),
                            ":sendingAt", n(sendingAtEpochMs)
                    ))
                    .build());
            log.debug("SENDING lock acquired: eventId={} channel={}", eventId, channel);
            return true;

        } catch (ConditionalCheckFailedException e) {
            log.debug("SENDING lock not acquired (already owned): eventId={} channel={}", eventId, channel);
            return false;

        } catch (DynamoDbException e) {
            log.error("Unexpected error acquiring SENDING lock: eventId={} channel={} error={}",
                    eventId, channel, e.getMessage(), e);
            throw e;
        }
    }

    // SENDING → SENT (with sesMessageId)

    public void markSent(String eventId, String channel, String sesMessageId) {
        dynamo.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key(eventId, channel))
                .updateExpression("SET #s = :sent, sesMessageId = :msgId")
                .expressionAttributeNames(Map.of("#s", ATTR_STATUS))
                .expressionAttributeValues(Map.of(
                        // Convert Enum to String
                        ":sent",  s(OutboxStatus.SENT.name()),
                        ":msgId", s(sesMessageId)
                ))
                .build());
        log.debug("Marked SENT: eventId={} sesMessageId={}", eventId, sesMessageId);
    }

    // SENDING → PENDING (retry / failure)

    public void markPendingForRetry(String eventId, String channel, int attempts, long nextRetryEpochMs) {
        dynamo.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key(eventId, channel))
                .updateExpression("SET #s = :pending, attempts = :attempts, nextRetry = :nextRetry")
                .expressionAttributeNames(Map.of("#s", ATTR_STATUS))
                .expressionAttributeValues(Map.of(
                        // Convert Enum to String
                        ":pending",   s(OutboxStatus.PENDING.name()),
                        ":attempts",  n(attempts),
                        ":nextRetry", n(nextRetryEpochMs)
                ))
                .build());
        log.debug("Marked PENDING for retry: eventId={} channel={} attempts={} nextRetry={}",
                eventId, channel, attempts, nextRetryEpochMs);
    }

    // DEAD

    public void markDead(String eventId, String channel) {
        dynamo.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key(eventId, channel))
                .updateExpression("SET #s = :dead")
                .expressionAttributeNames(Map.of("#s", ATTR_STATUS))
                .expressionAttributeValues(Map.of(
                        // Convert Enum to String
                        ":dead", s(OutboxStatus.DEAD.name())
                ))
                .build());
        log.warn("Marked DEAD (exhausted retries): eventId={} channel={}", eventId, channel);
    }

    // Read

    public Optional<OutboxRecord> findByKey(String eventId, String channel) {
        try {
            GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key(eventId, channel))
                    .build());

            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }

            Map<String, AttributeValue> item = response.item();
            long now = System.currentTimeMillis();
            OutboxRecord record = new OutboxRecord(eventId, channel, now);

            // Extract the string and convert it back to the Enum
            String statusString = item.getOrDefault(ATTR_STATUS, s(OutboxStatus.PENDING.name())).s();
            record.setStatus(OutboxStatus.valueOf(statusString));

            record.setAttempts(Integer.parseInt(item.getOrDefault(ATTR_ATTEMPTS, n(0)).n()));
            if (item.containsKey(ATTR_NEXT_RETRY))  record.setNextRetry(Long.parseLong(item.get(ATTR_NEXT_RETRY).n()));
            if (item.containsKey(ATTR_SENDING_AT))  record.setSendingAt(Long.parseLong(item.get(ATTR_SENDING_AT).n()));
            if (item.containsKey(ATTR_SES_MSG_ID))  record.setSesMessageId(item.get(ATTR_SES_MSG_ID).s());

            return Optional.of(record);

        } catch (DynamoDbException e) {
            log.error("Failed to fetch outbox record: eventId={} channel={} error={}",
                    eventId, channel, e.getMessage(), e);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.error("Failed to parse OutboxStatus enum from database string: eventId={} channel={}", eventId, channel, e);
            return Optional.empty();
        }
    }



    private static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue n(long value) {
        return AttributeValue.builder().n(String.valueOf(value)).build();
    }

    private static AttributeValue n(int value) {
        return AttributeValue.builder().n(String.valueOf(value)).build();
    }
}