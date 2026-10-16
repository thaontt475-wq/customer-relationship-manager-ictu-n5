package com.crm.dto.leads;

public class WebFormLeadRequest {

    private String requestId;
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    private String fullName;
    private String email;
    private String phone;
    private String company;
    private String companyName;
    private String interest;
    private String note;
    private String notes;
    private String source;
    private String formId;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;

    public WebFormLeadRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getUtmSource() {
        return utmSource;
    }

    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    public String getUtmMedium() {
        return utmMedium;
    }

    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    public String getUtmCampaign() {
        return utmCampaign;
    }

    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }

    /**
     * Resolves the company name from companyName or company.
     */
    public String getResolvedCompany() {
        if (companyName != null && !companyName.isBlank()) {
            return companyName.trim();
        }
        if (company != null && !company.isBlank()) {
            return company.trim();
        }
        return null;
    }

    /**
     * Resolves the interest/notes and appends UTM parameters if present.
     */
    public String getResolvedNotes() {
        StringBuilder sb = new StringBuilder();
        if (notes != null && !notes.isBlank()) {
            sb.append(notes.trim());
        } else if (interest != null && !interest.isBlank()) {
            sb.append(interest.trim());
        } else if (note != null && !note.isBlank()) {
            sb.append(note.trim());
        }

        StringBuilder utm = new StringBuilder();
        if (utmSource != null && !utmSource.isBlank()) {
            utm.append("utm_source=").append(utmSource.trim());
        }
        if (utmMedium != null && !utmMedium.isBlank()) {
            if (utm.length() > 0) utm.append(", ");
            utm.append("utm_medium=").append(utmMedium.trim());
        }
        if (utmCampaign != null && !utmCampaign.isBlank()) {
            if (utm.length() > 0) utm.append(", ");
            utm.append("utm_campaign=").append(utmCampaign.trim());
        }

        if (utm.length() > 0) {
            if (sb.length() > 0) {
                sb.append(" [Chiến dịch: ").append(utm).append("]");
            } else {
                sb.append("[Chiến dịch: ").append(utm).append("]");
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Resolves the explicitly supplied source; missing sources are rejected by the service.
     */
    public String getResolvedSource() {
        if (source != null && !source.isBlank()) {
            return source.trim();
        }
        return null;
    }
}
