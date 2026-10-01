package com.crm.model;

import java.sql.Timestamp;

/**
 * Optional filters supported by GET /api/audit-logs.
 */
public class AuditLogFilter {
    public static final int DEFAULT_LIMIT = 200;
    public static final int MAX_LIMIT = 500;

    private Long userId;
    private String objectType;
    private Long objectId;
    private Timestamp from;
    private Timestamp to;
    private int limit = DEFAULT_LIMIT;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public Timestamp getFrom() {
        return from;
    }

    public void setFrom(Timestamp from) {
        this.from = from;
    }

    public Timestamp getTo() {
        return to;
    }

    public void setTo(Timestamp to) {
        this.to = to;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
