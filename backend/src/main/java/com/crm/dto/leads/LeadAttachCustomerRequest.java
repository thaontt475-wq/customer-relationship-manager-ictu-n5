package com.crm.dto.leads;

public class LeadAttachCustomerRequest {
    public Long leadId;
    public Long customerId;
    public String notes;

    public LeadAttachCustomerRequest() {}

    public LeadAttachCustomerRequest(Long leadId, Long customerId, String notes) {
        this.leadId = leadId;
        this.customerId = customerId;
        this.notes = notes;
    }
}
