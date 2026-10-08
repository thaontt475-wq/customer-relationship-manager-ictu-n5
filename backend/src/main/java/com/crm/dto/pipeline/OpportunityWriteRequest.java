package com.crm.dto.pipeline;

import java.math.BigDecimal;
import java.util.Map;

public class OpportunityWriteRequest {
    private String name;
    private Long customerId;
    private String contactName;
    private BigDecimal amount;
    private Long stageId;
    private Integer probability;
    private String expectedCloseDate;
    private String status;
    private Long winReasonId;
    private Long lossReasonId;
    private Long competitorId;
    private String lostReason;
    private Long ownerUserId;
    private Map<String, Object> customFields;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public BigDecimal getAmount() {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public String getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(String expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public String getStatus() {
        return status != null && !status.isBlank() ? status : "OPEN";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWinReasonId() {
        return winReasonId;
    }

    public void setWinReasonId(Long winReasonId) {
        this.winReasonId = winReasonId;
    }

    public Long getLossReasonId() {
        return lossReasonId;
    }

    public void setLossReasonId(Long lossReasonId) {
        this.lossReasonId = lossReasonId;
    }

    public Long getCompetitorId() {
        return competitorId;
    }

    public void setCompetitorId(Long competitorId) {
        this.competitorId = competitorId;
    }

    public String getLostReason() {
        return lostReason;
    }

    public void setLostReason(String lostReason) {
        this.lostReason = lostReason;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
