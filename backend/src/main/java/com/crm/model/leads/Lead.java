package com.crm.model.leads;

import java.sql.Timestamp;

public class Lead {
    private Long id;
    private String fullName;
    private String companyName;
    private String email;
    private String phone;
    private String title;
    private Long sourceId;
    private String source;
    private String region;
    private String regionCode;
    private String industry;
    private Integer employeeCount;
    private Integer score;
    private String notes;
    private String status;
    private String formId;
    private String ipAddress;
    private Long ownerUserId;
    private Long assignedTo;
    private String assignedToStr;
    private String assignedToRepId;
    private Long assignedToUserId;
    private Timestamp assignedAt;
    private Integer slaTotalMinutes;
    private Timestamp slaDeadline;
    private Timestamp acceptedAt;
    private Timestamp rejectedAt;
    private String rejectReason;
    private String rejectReasonLabel;
    private String rejectDetails;
    private String slaResult;
    private String responseTimeText;
    private String contactingStage;
    private String unassignedReason;
    private boolean isLocked;
    private boolean isDeleted;
    private Long createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

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
            this.assignedToRepId = null;
        }
    }

    public String getAssignedToStr() {
        if (assignedToStr != null) return assignedToStr;
        if (assignedTo != null) return "USR-" + assignedTo;
        if (assignedToRepId != null) return assignedToRepId;
        return null;
    }

    public String getAssignedToRepId() { return assignedToRepId; }
    public void setAssignedToRepId(String assignedToRepId) { this.assignedToRepId = assignedToRepId; }

    public Long getAssignedToUserId() {
        if (assignedToUserId != null) return assignedToUserId;
        if (assignedTo != null) return assignedTo;
        return null;
    }
    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
        if (assignedToUserId != null && this.assignedTo == null) {
            this.assignedTo = assignedToUserId;
            this.assignedToRepId = String.valueOf(assignedToUserId);
            this.assignedToStr = "USR-" + assignedToUserId;
        }
    }

    public Timestamp getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Timestamp assignedAt) { this.assignedAt = assignedAt; }

    public Integer getSlaTotalMinutes() { return slaTotalMinutes; }
    public void setSlaTotalMinutes(Integer slaTotalMinutes) { this.slaTotalMinutes = slaTotalMinutes; }

    public Timestamp getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Timestamp slaDeadline) { this.slaDeadline = slaDeadline; }

    public Timestamp getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Timestamp acceptedAt) { this.acceptedAt = acceptedAt; }

    public Timestamp getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Timestamp rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public String getRejectReasonLabel() { return rejectReasonLabel; }
    public void setRejectReasonLabel(String rejectReasonLabel) { this.rejectReasonLabel = rejectReasonLabel; }

    public String getRejectDetails() { return rejectDetails; }
    public void setRejectDetails(String rejectDetails) { this.rejectDetails = rejectDetails; }

    public String getSlaResult() { return slaResult; }
    public void setSlaResult(String slaResult) { this.slaResult = slaResult; }

    public String getResponseTimeText() { return responseTimeText; }
    public void setResponseTimeText(String responseTimeText) { this.responseTimeText = responseTimeText; }

    public String getContactingStage() { return contactingStage; }
    public void setContactingStage(String contactingStage) { this.contactingStage = contactingStage; }

    public String getUnassignedReason() { return unassignedReason; }
    public void setUnassignedReason(String unassignedReason) { this.unassignedReason = unassignedReason; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
}
