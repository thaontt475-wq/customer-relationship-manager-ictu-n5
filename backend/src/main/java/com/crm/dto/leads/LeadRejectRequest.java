package com.crm.dto.leads;

public class LeadRejectRequest {
    private String reason;
    private String details;

    public LeadRejectRequest() {}

    public LeadRejectRequest(String reason, String details) {
        this.reason = reason;
        this.details = details;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
