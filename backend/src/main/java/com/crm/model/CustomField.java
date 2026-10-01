package com.crm.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Definition of a CRM-46 custom field. */
public class CustomField {
    private Long id;
    private String entityType;
    private String fieldName;
    private String fieldLabel;
    /** API value: TEXT, NUMBER, DATE or DROPDOWN (SELECT is accepted as input). */
    private String fieldType;
    private boolean isRequired;
    private List<String> options = new ArrayList<>();
    private int sortOrder;
    private boolean active = true;
    private boolean inForm = true;
    private boolean inFilter = true;
    private boolean inExport = true;
    private long usageCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) {
        this.fieldType = "SELECT".equalsIgnoreCase(fieldType) ? "DROPDOWN" : fieldType;
    }
    public String getStorageType() {
        return "DROPDOWN".equalsIgnoreCase(fieldType) ? "SELECT" : fieldType;
    }
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) {
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
    }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isInForm() { return inForm; }
    public void setInForm(boolean inForm) { this.inForm = inForm; }
    public boolean isInFilter() { return inFilter; }
    public void setInFilter(boolean inFilter) { this.inFilter = inFilter; }
    public boolean isInExport() { return inExport; }
    public void setInExport(boolean inExport) { this.inExport = inExport; }
    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

}
