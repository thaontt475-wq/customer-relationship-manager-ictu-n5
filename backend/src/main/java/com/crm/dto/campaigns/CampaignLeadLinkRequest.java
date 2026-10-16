package com.crm.dto.campaigns;

import java.util.List;

public class CampaignLeadLinkRequest {
    private List<Long> leadIds;
    private String status;
    private String notes;

    public CampaignLeadLinkRequest() {
    }

    public List<Long> getLeadIds() {
        return leadIds;
    }

    public void setLeadIds(List<Long> leadIds) {
        this.leadIds = leadIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
