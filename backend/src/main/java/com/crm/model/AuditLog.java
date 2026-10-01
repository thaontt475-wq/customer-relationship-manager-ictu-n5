package com.crm.model;

import com.google.gson.JsonElement;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Immutable change history entry for CRM-37.
 */
public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private long actorUserId;
    private String action;
    private String objectType;
    private long objectId;
    private JsonElement beforeValue;
    private JsonElement afterValue;
    private Timestamp createdAt;

    public AuditLog() {
    }

    public AuditLog(long actorUserId, String action, String objectType, long objectId,
                    JsonElement beforeValue, JsonElement afterValue) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.objectType = objectType;
        this.objectId = objectId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public JsonElement getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(JsonElement beforeValue) {
        this.beforeValue = beforeValue;
    }

    public JsonElement getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(JsonElement afterValue) {
        this.afterValue = afterValue;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

}
