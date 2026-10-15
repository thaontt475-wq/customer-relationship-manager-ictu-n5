package com.crm.model.leads;

import java.sql.Timestamp;

public class Lead {
    private Long id;
    private String code;
    private String fullName;
    private String title;
    private String companyName;
    private String industry;
    private String taxCode;
    private String phone;
    private String email;
    private String source;
    private String sourceName;
    private String tier = "WARM";
    private String rating = "WARM";
    private int score = 50;
    private String status = "NEW";
    private Long assignedTo;
    private String owner;
    private Long ownerId;
    private double estimatedValue;
    private Timestamp slaDeadline;
    private int slaMinutesTotal = 120;
    private Timestamp lastContactedAt;
    private String notes;
    private String formId;
    private String ipAddress;
    private String region;
    private String regionCode;
    private String unassignedReason;
    private boolean isDeleted;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Computed SLA fields (S4-09 AC 2)
    private boolean isOverdueSla;
    private Long slaRemainingMinutes;
    private String slaStatus; // "OVERDUE", "SAFE", "WARNING", "CLOSED"
    private String slaRemainingText;
    private String timeAgo;

    public Lead() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code != null ? code : (id != null ? String.format("LEAD-2026-%03d", id) : null);
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceName() {
        if (sourceName != null && !sourceName.isBlank()) return sourceName;
        if (source == null) return "Chưa xác định";
        return switch (source.toUpperCase()) {
            case "WEBSITE", "WEB_FORM" -> "Website Form";
            case "EVENT" -> "Hội thảo / Sự kiện";
            case "CAMPAIGN" -> "Chiến dịch Marketing";
            case "REFERRAL" -> "Giới thiệu / Đối tác";
            case "HOTLINE" -> "Tổng đài Hotline";
            case "EXCEL" -> "Nhập từ Excel";
            case "FACEBOOK" -> "Facebook Ads";
            default -> source;
        };
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getTier() {
        return tier != null ? tier : (rating != null ? rating : "WARM");
    }

    public void setTier(String tier) {
        this.tier = tier;
        this.rating = tier;
    }

    public String getRating() {
        return getTier();
    }

    public void setRating(String rating) {
        setTier(rating);
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
        this.ownerId = assignedTo;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getOwnerId() {
        return ownerId != null ? ownerId : assignedTo;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
        this.assignedTo = ownerId;
    }

    public double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public Timestamp getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(Timestamp slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public int getSlaMinutesTotal() {
        return slaMinutesTotal;
    }

    public void setSlaMinutesTotal(int slaMinutesTotal) {
        this.slaMinutesTotal = slaMinutesTotal;
    }

    public Timestamp getLastContactedAt() {
        return lastContactedAt;
    }

    public void setLastContactedAt(Timestamp lastContactedAt) {
        this.lastContactedAt = lastContactedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getUnassignedReason() {
        return unassignedReason;
    }

    public void setUnassignedReason(String unassignedReason) {
        this.unassignedReason = unassignedReason;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
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

    public boolean isOverdueSla() {
        return isOverdueSla;
    }

    public void setOverdueSla(boolean overdueSla) {
        isOverdueSla = overdueSla;
    }

    public Long getSlaRemainingMinutes() {
        return slaRemainingMinutes;
    }

    public void setSlaRemainingMinutes(Long slaRemainingMinutes) {
        this.slaRemainingMinutes = slaRemainingMinutes;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public String getSlaRemainingText() {
        return slaRemainingText;
    }

    public void setSlaRemainingText(String slaRemainingText) {
        this.slaRemainingText = slaRemainingText;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }
}
