package com.osrsGoalTracker.notificationChannel.dao.internal.ddb.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.osrsGoalTracker.dao.goalTracker.exception.ResourceNotFoundException;
import com.osrsGoalTracker.dao.goalTracker.internal.ddb.util.SortKeyUtil;
import com.osrsGoalTracker.notificationChannel.dao.NotificationChannelDao;
import com.osrsGoalTracker.notificationChannel.dao.entity.NotificationChannelEntity;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * DynamoDB implementation for notification channel-related operations.
 * Handles creating and retrieving notification channels.
 */
@Slf4j
public class DynamoNotificationChannelDao implements NotificationChannelDao {
    private static final String PK = "pk";
    private static final String SK = "sk";
    private static final String USER_PREFIX = "USER#";

    private static final String CHANNEL_TYPE = "channelType";
    private static final String IDENTIFIER = "identifier";
    private static final String IS_ACTIVE = "isActive";
    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_AT = "updatedAt";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    /**
     * Constructor for DynamoNotificationDao.
     *
     * @param dynamoDbClient The AWS DynamoDB client
     * @param tableName      The name of the DynamoDB table
     */
    public DynamoNotificationChannelDao(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    private void validateNotificationChannel(NotificationChannelEntity channel) {
        if (channel == null) {
            log.warn("Attempted to create null notification channel");
            throw new IllegalArgumentException("Notification channel entity cannot be null");
        }
        if (channel.getChannelType() == null || channel.getChannelType().trim().isEmpty()) {
            log.warn("Attempted to create notification channel with null or empty channel type");
            throw new IllegalArgumentException("Channel type cannot be null or empty");
        }
        if (channel.getIdentifier() == null || channel.getIdentifier().trim().isEmpty()) {
            log.warn("Attempted to create notification channel with null or empty identifier");
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
    }

    /**
     * Creates a new notification channel for a user.
     *
     * @param userId  The ID of the user
     * @param channel The notification channel entity to create
     * @return The created notification channel entity with timestamps
     * @throws IllegalArgumentException If userId is null/empty or channel
     *                                  validation fails
     */
    public NotificationChannelEntity createNotificationChannel(String userId, NotificationChannelEntity channel) {
        log.debug("Creating notification channel for user {}: {}", userId, channel);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Attempted to create notification channel with null or empty user ID");
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }

        validateNotificationChannel(channel);

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DATE_TIME_FORMATTER);

        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put(PK, AttributeValue.builder().s(USER_PREFIX + userId).build());
        item.put(SK, AttributeValue.builder().s(SortKeyUtil.getNotificationChannelSortKey(channel.getChannelType()))
                .build());
        item.put(CHANNEL_TYPE, AttributeValue.builder().s(channel.getChannelType()).build());
        item.put(IDENTIFIER, AttributeValue.builder().s(channel.getIdentifier()).build());
        item.put(IS_ACTIVE, AttributeValue.builder().bool(channel.isActive()).build());
        item.put(CREATED_AT, AttributeValue.builder().s(timestamp).build());
        item.put(UPDATED_AT, AttributeValue.builder().s(timestamp).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        log.debug("Putting new notification channel in DynamoDB for user: {}", userId);
        dynamoDbClient.putItem(putItemRequest);
        log.info("Successfully created notification channel for user {} of type {}", userId, channel.getChannelType());

        return NotificationChannelEntity.builder()
                .userId(userId)
                .channelType(channel.getChannelType())
                .identifier(channel.getIdentifier())
                .isActive(channel.isActive())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Retrieves all notification channels for a user.
     *
     * @param userId The ID of the user
     * @return List of notification channel entities
     * @throws IllegalArgumentException  If userId is null or empty
     * @throws ResourceNotFoundException If user is not found
     */
    public List<NotificationChannelEntity> getNotificationChannels(String userId) {
        log.debug("Getting notification channels for user: {}", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Attempted to get notification channels with null or empty user ID");
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }

        Map<String, AttributeValue> expressionAttributeValues = new LinkedHashMap<>();
        expressionAttributeValues.put(":pk", AttributeValue.builder().s(USER_PREFIX + userId).build());
        expressionAttributeValues.put(":sk_prefix", AttributeValue.builder().s("NOTIFICATION#").build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("pk = :pk AND begins_with(sk, :sk_prefix)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);
        List<NotificationChannelEntity> channels = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            channels.add(NotificationChannelEntity.builder()
                    .userId(userId)
                    .channelType(item.get(CHANNEL_TYPE).s())
                    .identifier(item.get(IDENTIFIER).s())
                    .isActive(item.get(IS_ACTIVE).bool())
                    .createdAt(LocalDateTime.parse(item.get(CREATED_AT).s(), DATE_TIME_FORMATTER))
                    .updatedAt(LocalDateTime.parse(item.get(UPDATED_AT).s(), DATE_TIME_FORMATTER))
                    .build());
        }

        log.debug("Retrieved {} notification channels for user {}", channels.size(), userId);
        return channels;
    }
}