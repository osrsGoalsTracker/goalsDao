
# DynamoDB Schema for Goal Tracking and Notification System

## Overview

This schema is designed to support a goal tracking and notification platform for RuneScape players. The system allows users to:
- Create and manage RuneScape-related goals.
- Track progress towards goals over time.
- Configure notification channels for goal updates.
- Support efficient queries for metadata, progress tracking, and notifications.

### Key Features
1. **Flexible Data Organization**: Supports storing metadata, goals, progress, and notifications using structured keys.
2. **Efficient Access Patterns**: Enables querying for user-specific data, goals, and progress.
3. **Scalable Design**: Accommodates daily progress updates for multiple goals while adhering to DynamoDB’s scalability limits.
4. **Low Maintenance**: Designed with minimal data duplication and optional TTL for data retention.

---

## DynamoDB Schema

### Primary Table: Goals Table
- **Partition Key (PK):** `USER#<user_id>`
- **Sort Key (SK):** Encodes various entity types and their metadata/data using structured prefixes.

---

### Sort Key Structure and Examples

#### 1. **User Metadata**
   - **Sort Key:** `METADATA`
   - **Purpose:** This is the metadata for the user. It is used to store information about the user such as their email, createdAt, and updatedAt.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "METADATA",
       "email": "user@example.com",
       "createdAt": "2025-01-01T00:00:00Z",
       "updatedAt": "2025-01-01T00:00:00Z"
     }
     ```

#### 2. **Notification Channels**
   - **Sort Key:** `NOTIFICATION#<channel_type>`
   - **Purpose:** This is the metadata for the notification channels for the user. It is used to store information about the notification channels for the user such as the channel type (e.g. SMS, Discord, etc.), identifier (e.g. phone number, discord channel id, etc.), createdAt, and isActive.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "NOTIFICATION#SMS",
       "channelType": "SMS",
       "identifier": "+1234567890",
       "createdAt": "2025-01-01T00:00:00Z",
       "updatedAt": "2025-01-01T00:00:00Z",
       "isActive": true
     }
     ```

#### 3. **RSN Metadata**
   - **Sort Key:** `RSN#METADATA#<rsn>`
   - **Purpose:** This is the metadata for the runescape account. It is used to store information about the runescape account such as the RSN, createdAt, and updatedAt.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "RSN#METADATA#Player123",
       "rsn": "Player123",
       "createdAt": "2025-01-01T00:00:00Z",
       "updatedAt": "2025-01-01T00:00:00Z"
     }
     ```

#### 4. **Goal Metadata**
   - **Sort Key:** `RSN#<rsn>#GOAL#METADATA#<skill/activity>`
   - **Purpose:** This is the metadata for the goal. It is used to store information about the goal such as the RSN, skill/activity, targetXp, targetDate, notificationChannels, frequency, createdAt, and updatedAt.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "RSN#Player123#GOAL#METADATA#Woodcutting",
       "rsn": "Player123",
       "skill": "Woodcutting",
       "targetType": "xp",
       "targetValue": 13034431,
       "targetDate": "2025-03-01",
       "notificationChannels": ["SMS", "Discord"],
       "frequency": "daily",
       "createdAt": "2025-01-01T00:00:00Z",
       "updatedAt": "2025-01-01T00:00:00Z"
     }
     ```

#### 5. **Progress Records**
   - **Sort Key:** `RSN#<rsn>#GOAL#<skill/activity>#<timestamp>`
   - **Purpose:** This is the progress record for the goal. It is used to store information about the progress for the goal such as the progress value, timestamp, and createdAt.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "RSN#Player123#GOAL#Woodcutting#2025-01-01T00:00:00Z",
       "progressValue": 12000000,
       "timestamp": "2025-01-01T00:00:00Z",
       "createdAt": "2025-01-01T00:00:00Z",
       "updatedAt": "2025-01-01T00:00:00Z"
     }
     ```

#### 6. **Latest Progress**
   - **Sort Key:** `RSN#<rsn>#GOAL#<skill/activity>#LATEST`
   - **Purpose:** This is the latest progress record for the goal. It is used to store information about the latest progress for the goal such as the progress value, timestamp, and createdAt. This provides a quick way to get the latest progress for a goal.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "RSN#Player123#GOAL#Woodcutting#LATEST",
       "progressValue": 12500000,
       "timestamp": "2025-01-02T00:00:00Z",
       "createdAt": "2025-01-02T00:00:00Z",
       "updatedAt": "2025-01-02T00:00:00Z"
     }
     ```  

#### 7. **Earliest Progress**
   - **Sort Key:** `RSN#<rsn>#GOAL#<skill/activity>#EARLIEST`
   - **Purpose:** This is the earliest progress record for the goal. It is used to store information about the earliest progress for the goal such as the timestamp, and createdAt. This provides a quick way to get the earliest progress for a goal.
   - **Example Item:**
     ```json
     {
       "PK": "USER#12345",
       "SK": "RSN#Player123#GOAL#Woodcutting#EARLIEST",
       "timestamp": "2025-01-01T00:00:00Z",
       "createdAt": "2025-01-01T00:00:00Z",
       "updatedAt": "2025-01-01T00:00:00Z"
     }
     ```

---

### Indexes

#### Primary Index
- **PK:** `USER#<user_id>`
- **SK:** Encodes metadata, notification channels, goals, and progress.

---

### Additional Considerations

1. **Pagination:**
   - Use `EARLIEST` and `LATEST` rows for efficient paginated queries of historical progress.

2. **Data Retention:**
   - Implement TTL for progress records if long-term storage becomes costly or unnecessary.

3. **Consistency:**
   - Eventual consistency is sufficient for the use case, minimizing costs.

4. **Attribute Size:**
   - All attributes are small (e.g., `C2U2` fields, notification type lists) and well below DynamoDB's item size limit of 400 KB.

5. **Fetching all goals for a user+RSN**
   - Store a list of tracked skills on the user#RSN metadata field. Should it be a list or something else? Would be nice to see the most tracked skills

---

This schema provides a robust and scalable foundation for the goal tracking and notification system. It supports key use cases while remaining flexible for future enhancements.