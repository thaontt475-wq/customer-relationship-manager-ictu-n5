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
    private String source;
    private String sourceName;
    private String tier = "WARM";
    private String rating = "WARM";
    private Integer score = 50;
    private String status = "NEW";
    private String assignedTo;
    private Long assignedToUserId;
    private String assignedToRepId;
    private Timestamp assignedAt;
    private String owner;
    private Long ownerId;
    private Long ownerUserId;
    private double estimatedValue;
    private Integer slaTotalMinutes = 120;
    private int slaMinutesTotal = 120;
    private Timestamp slaDeadline;
    private Timestamp acceptedAt;
    private Timestamp rejectedAt;
    private String rejectReason;
    private String rejectReasonLabel;
    private String rejectDetails;
    private String slaResult;
    private String responseTimeText;
    private String contactingStage;
    private Integer employeeCount;
    private boolean isLocked;
    private Long createdBy;
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
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getTier() {
        return tier != null ? tier : rating;
    }

    public void setTier(String tier) {
        this.tier = tier;
        if (this.rating == null || this.rating.isBlank()) {
            this.rating = tier;
        }
    }

    public String getRating() {
        return rating != null ? rating : tier;
    }

    public void setRating(String rating) {
        this.rating = rating;
        if (this.tier == null || this.tier.isBlank()) {
            this.tier = rating;
        }
    }

    public Integer getScore() {
        return score != null ? score : 0;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssignedTo() {
        if (assignedToUserId != null) return assignedToUserId;
        if (assignedTo != null) {
            try {
                String digits = assignedTo.replaceAll("[^0-9]", "");
                if (!digits.isBlank()) return Long.parseLong(digits);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedToUserId = assignedTo;
        this.assignedTo = assignedTo != null ? String.valueOf(assignedTo) : null;
    }

    public void setAssignedTo(Object val) {
        if (val == null) {
            this.assignedToUserId = null;
            this.assignedTo = null;
        } else if (val instanceof Number n) {
            setAssignedTo(n.longValue());
        } else {
            String s = val.toString();
            this.assignedTo = s;
            try {
                String digits = s.replaceAll("[^0-9]", "");
                if (!digits.isBlank()) {
                    this.assignedToUserId = Long.parseLong(digits);
                }
            } catch (Exception ignored) {}
        }
    }

    public Long getAssignedToUserId() {
        return getAssignedTo();
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        setAssignedTo(assignedToUserId);
    }

    public String getAssignedToRepId() {
        return assignedToRepId;
    }

    public void setAssignedToRepId(String assignedToRepId) {
        this.assignedToRepId = assignedToRepId;
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getOwnerId() {
        return ownerId != null ? ownerId : ownerUserId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
        this.ownerUserId = ownerId;
    }

    public Long getOwnerUserId() {
        return ownerUserId != null ? ownerUserId : ownerId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
        this.ownerId = ownerUserId;
    }

    public double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
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
        return slaTotalMinutes != null ? slaTotalMinutes : slaMinutesTotal;
    }

    public void setSlaMinutesTotal(int slaMinutesTotal) {
        this.slaMinutesTotal = slaMinutesTotal;
        this.slaTotalMinutes = slaMinutesTotal;
    }

    public Timestamp getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(Timestamp slaDeadline) {
        this.slaDeadline = slaDeadline;
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

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
