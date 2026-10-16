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
    private Long sourceId;
    private String source = "WEB_FORM";
    private String sourceName;
    private Integer employeeCount;
    private String tier = "WARM";
    private String rating = "WARM";
    private Integer score = 50;
    private String status = "NEW";
    private String formId;
    private String ipAddress;
    private String region;
    private String regionCode;
    private Long ownerUserId;
    private Long ownerId;
    private String owner;
    private Long assignedTo;
    private String assignedToStr;
    private String assignedToRepId;
    private Long assignedToUserId;
    private Timestamp assignedAt;
    private double estimatedValue;
    private Timestamp slaDeadline;
    private Integer slaTotalMinutes = 120;
    private int slaMinutesTotal = 120;
    private Timestamp lastContactedAt;
    private Timestamp acceptedAt;
    private Timestamp rejectedAt;
    private String rejectReason;
    private String rejectReasonLabel;
    private String rejectDetails;
    private String slaResult;
    private String responseTimeText;
    private String contactingStage;
    private String unassignedReason;
    private String notes;
    private boolean isLocked;
    private boolean isDeleted;
    private Long createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Computed SLA fields (S4-09 AC 2)
    private boolean isOverdueSla;
    private Long slaRemainingMinutes;
    private String slaStatus; // "OVERDUE", "SAFE", "WARNING", "CLOSED"
    private String slaRemainingText;
    private String timeAgo;

    public Lead() {
        this.status = "NEW";
        this.source = "WEB_FORM";
    }

    public Lead(String fullName, String email, String phone, String companyName, String notes) {
        this();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.companyName = companyName;
        this.notes = notes;
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

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
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

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
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

    public Integer getScore() {
        return score != null ? score : 50;
    }

    public void setScore(Integer score) {
        this.score = score;
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

    public Long getOwnerUserId() {
        return getOwnerId();
    }

    public void setOwnerUserId(Long ownerUserId) {
        setOwnerId(ownerUserId);
    }

    public Long getOwnerId() {
        if (ownerId != null) return ownerId;
        if (ownerUserId != null) return ownerUserId;
        return assignedTo;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
        this.ownerUserId = ownerId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getAssignedTo() {
        if (assignedTo != null) return assignedTo;
        if (assignedToUserId != null) return assignedToUserId;
        return null;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
        if (assignedTo != null) {
            this.assignedToUserId = assignedTo;
            this.assignedToStr = "USR-" + assignedTo;
            this.assignedToRepId = String.valueOf(assignedTo);
        } else {
            this.assignedToUserId = null;
            this.assignedToStr = null;
            this.assignedToRepId = null;
        }
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedToStr = assignedTo;
        if (assignedTo != null) {
            this.assignedToRepId = assignedTo;
            try {
                long uid = Long.parseLong(assignedTo.replace("USR-", "").trim());
                this.assignedTo = uid;
                this.assignedToUserId = uid;
            } catch (Exception ignored) {}
        } else {
            this.assignedTo = null;
            this.assignedToUserId = null;
            this.assignedToRepId = null;
        }
    }

    public String getAssignedToStr() {
        if (assignedToStr != null) return assignedToStr;
        if (assignedTo != null) return "USR-" + assignedTo;
        if (assignedToRepId != null) return assignedToRepId;
        return null;
    }

    public String getAssignedToRepId() {
        return assignedToRepId;
    }

    public void setAssignedToRepId(String assignedToRepId) {
        this.assignedToRepId = assignedToRepId;
    }

    public Long getAssignedToUserId() {
        if (assignedToUserId != null) return assignedToUserId;
        if (assignedTo != null) return assignedTo;
        return null;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
        if (assignedToUserId != null) {
            if (this.assignedTo == null) {
                this.assignedTo = assignedToUserId;
            }
            this.assignedToRepId = String.valueOf(assignedToUserId);
            this.assignedToStr = "USR-" + assignedToUserId;
        } else {
            this.assignedTo = null;
            this.assignedToRepId = null;
            this.assignedToStr = null;
        }
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
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

    public Integer getSlaTotalMinutes() {
        return slaTotalMinutes != null ? slaTotalMinutes : slaMinutesTotal;
    }

    public void setSlaTotalMinutes(Integer slaTotalMinutes) {
        this.slaTotalMinutes = slaTotalMinutes;
        if (slaTotalMinutes != null) {
            this.slaMinutesTotal = slaTotalMinutes;
        }
    }

    public int getSlaMinutesTotal() {
        return slaMinutesTotal;
    }

    public void setSlaMinutesTotal(int slaMinutesTotal) {
        this.slaMinutesTotal = slaMinutesTotal;
        this.slaTotalMinutes = slaMinutesTotal;
    }

    public Timestamp getLastContactedAt() {
        return lastContactedAt;
    }

    public void setLastContactedAt(Timestamp lastContactedAt) {
        this.lastContactedAt = lastContactedAt;
    }

    public Timestamp getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Timestamp acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Timestamp getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Timestamp rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRejectReasonLabel() {
        return rejectReasonLabel;
    }

    public void setRejectReasonLabel(String rejectReasonLabel) {
        this.rejectReasonLabel = rejectReasonLabel;
    }

    public String getRejectDetails() {
        return rejectDetails;
    }

    public void setRejectDetails(String rejectDetails) {
        this.rejectDetails = rejectDetails;
    }

    public String getSlaResult() {
        return slaResult;
    }

    public void setSlaResult(String slaResult) {
        this.slaResult = slaResult;
    }

    public String getResponseTimeText() {
        return responseTimeText;
    }

    public void setResponseTimeText(String responseTimeText) {
        this.responseTimeText = responseTimeText;
    }

    public String getContactingStage() {
        return contactingStage;
    }

    public void setContactingStage(String contactingStage) {
        this.contactingStage = contactingStage;
    }

    public String getUnassignedReason() {
        return unassignedReason;
    }

    public void setUnassignedReason(String unassignedReason) {
        this.unassignedReason = unassignedReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isLocked() {
        return isLocked || "CONVERTED".equalsIgnoreCase(status);
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
