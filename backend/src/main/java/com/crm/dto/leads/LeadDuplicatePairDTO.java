package com.crm.dto.leads;

import java.util.List;
import java.util.Map;

public class LeadDuplicatePairDTO {
    private String id;
    private String criterion; // PHONE, EMAIL, COMPANY, CUSTOMER
    private int confidence; // 0-100
    private String detectedAt; // ISO-8601 string
    private Map<String, Object> recordA;
    private Map<String, Object> recordB;
    private boolean isCustomerMatch;
    private List<String> matchedFields;

    public LeadDuplicatePairDTO() {}

    public LeadDuplicatePairDTO(
            String id,
            String criterion,
            int confidence,
            String detectedAt,
            Map<String, Object> recordA,
            Map<String, Object> recordB,
            boolean isCustomerMatch,
            List<String> matchedFields
    ) {
        this.id = id;
        this.criterion = criterion;
        this.confidence = confidence;
        this.detectedAt = detectedAt;
        this.recordA = recordA;
        this.recordB = recordB;
        this.isCustomerMatch = isCustomerMatch;
        this.matchedFields = matchedFields;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCriterion() { return criterion; }
    public void setCriterion(String criterion) { this.criterion = criterion; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }

    public String getDetectedAt() { return detectedAt; }
    public void setDetectedAt(String detectedAt) { this.detectedAt = detectedAt; }

    public Map<String, Object> getRecordA() { return recordA; }
    public void setRecordA(Map<String, Object> recordA) { this.recordA = recordA; }

    public Map<String, Object> getRecordB() { return recordB; }
    public void setRecordB(Map<String, Object> recordB) { this.recordB = recordB; }

    public boolean isCustomerMatch() { return isCustomerMatch; }
    public void setCustomerMatch(boolean customerMatch) { isCustomerMatch = customerMatch; }

    public List<String> getMatchedFields() { return matchedFields; }
    public void setMatchedFields(List<String> matchedFields) { this.matchedFields = matchedFields; }
}
