package com.crm.dto.leads;

import java.util.Map;

public class LeadMergeRequest {
    public Long masterId;
    public Long duplicateId;
    public Map<String, Object> mergedData;
    public Boolean retainHistory;

    public LeadMergeRequest() {}

    public LeadMergeRequest(Long masterId, Long duplicateId, Map<String, Object> mergedData, Boolean retainHistory) {
        this.masterId = masterId;
        this.duplicateId = duplicateId;
        this.mergedData = mergedData;
        this.retainHistory = retainHistory;
    }
}
