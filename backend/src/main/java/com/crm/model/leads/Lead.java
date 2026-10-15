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
    private Long ownerUserId;
    private String assignedTo;
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
    private Long createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Lead() {}

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

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public Long getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(Long assignedToUserId) { this.assignedToUserId = assignedToUserId; }

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

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
