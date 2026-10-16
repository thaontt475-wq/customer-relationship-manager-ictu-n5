package com.crm.model.leads;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AssignmentRule {
    private Long id;
    private String ruleCode;
    private String name;
    private int priority;
    private String region;
    private String industry;
    private String mechanism;
    private List<String> reps = new ArrayList<>();
    private String description;
    private boolean isActive = true;
    private int totalAssigned;
    private int lastAssignedIndex;
    private Long lastAssignedUserId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public AssignmentRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriorityOrder() {
        return priority;
    }

    public void setPriorityOrder(int priorityOrder) {
        this.priority = priorityOrder;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getMechanism() {
        return mechanism;
    }

    public void setMechanism(String mechanism) {
        this.mechanism = mechanism;
    }

    public List<String> getReps() {
        if (reps == null) {
            reps = new ArrayList<>();
        }
        return reps;
    }

    public void setReps(List<String> reps) {
        this.reps = reps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getTotalAssigned() {
        return totalAssigned;
    }

    public void setTotalAssigned(int totalAssigned) {
        this.totalAssigned = totalAssigned;
    }

    public int getLastAssignedIndex() {
        return lastAssignedIndex;
    }

    public void setLastAssignedIndex(int lastAssignedIndex) {
        this.lastAssignedIndex = lastAssignedIndex;
    }

    public Long getLastAssignedUserId() {
        return lastAssignedUserId;
    }

    public void setLastAssignedUserId(Long lastAssignedUserId) {
        this.lastAssignedUserId = lastAssignedUserId;
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
