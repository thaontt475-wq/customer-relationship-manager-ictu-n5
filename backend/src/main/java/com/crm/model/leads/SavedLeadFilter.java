package com.crm.model.leads;

import java.sql.Timestamp;

public class SavedLeadFilter {
    private Object id;
    private Long userId;
    private String name;
    private Object filter;
    private String filterCriteria;
    private boolean isDefault;
    private boolean isSystem;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public SavedLeadFilter() {
    }

    public SavedLeadFilter(Object id, Long userId, String name, Object filter, boolean isDefault, boolean isSystem) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.filter = filter;
        this.isDefault = isDefault;
        this.isSystem = isSystem;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getFilter() {
        return filter;
    }

    public void setFilter(Object filter) {
        this.filter = filter;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
