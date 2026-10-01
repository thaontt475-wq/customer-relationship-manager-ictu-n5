package com.crm.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Values of all custom fields for one CRM record. */
public class CustomFieldValues {
    private String entityType;
    private long recordId;
    private Map<String, String> values = new LinkedHashMap<>();

    public CustomFieldValues() { }

    public CustomFieldValues(String entityType, long recordId, Map<String, String> values) {
        this.entityType = entityType;
        this.recordId = recordId;
        this.values = new LinkedHashMap<>(values);
    }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public long getRecordId() { return recordId; }
    public void setRecordId(long recordId) { this.recordId = recordId; }
    public Map<String, String> getValues() { return values; }
    public void setValues(Map<String, String> values) {
        this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }
}
