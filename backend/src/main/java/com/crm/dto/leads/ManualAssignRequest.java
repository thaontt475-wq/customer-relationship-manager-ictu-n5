package com.crm.dto.leads;

public class ManualAssignRequest {
    private String leadId;
    private String repId;
    private String note;

    public ManualAssignRequest() {
    }

    public ManualAssignRequest(String leadId, String repId, String note) {
        this.leadId = leadId;
        this.repId = repId;
        this.note = note;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getRepId() {
        return repId;
    }

    public void setRepId(String repId) {
        this.repId = repId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getParsedLeadId() {
        if (leadId == null || leadId.isBlank()) return null;
        try {
            String digits = leadId.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
