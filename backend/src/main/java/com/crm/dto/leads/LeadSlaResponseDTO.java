package com.crm.dto.leads;

import com.crm.model.leads.Lead;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class LeadSlaResponseDTO {
    private Long id;
    private String fullName;
    private String title;
    private String phone;
    private String email;
    private String companyName;
    private String industry;
    private String region;
    private Integer employeeCount;
    private Integer score;
    private String assignedAt;
    private Integer slaTotalMinutes;
    private Long slaDeadline;
    private String status;
    private String acceptedAt;
    private String rejectedAt;
    private String rejectReason;
    private String rejectReasonLabel;
    private String rejectDetails;
    private String slaResult;
    private String responseTimeText;
    private String contactingStage;

    private static final Map<String, String> REASON_LABELS = Map.of(
        "INVALID_PHONE", "Sai số điện thoại / Không liên lạc được",
        "OUT_OF_TERRITORY", "Ngoài địa bàn / Khu vực tôi phụ trách",
        "OVERLOADED", "Đang quá tải công việc / Không đảm bảo SLA",
        "NO_INTENT", "Khách hàng nhầm số / Không có nhu cầu CRM",
        "COMPETITOR", "Đối thủ cạnh tranh khảo giá",
        "OTHER", "Lý do nghiệp vụ khác"
    );

    public LeadSlaResponseDTO() {}

    public static LeadSlaResponseDTO fromModel(Lead lead) {
        LeadSlaResponseDTO dto = new LeadSlaResponseDTO();
        dto.id = lead.getId();
        dto.fullName = lead.getFullName();
        dto.title = lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng";
        dto.phone = lead.getPhone();
        dto.email = lead.getEmail();
        dto.companyName = lead.getCompanyName();
        dto.industry = lead.getIndustry() != null ? lead.getIndustry() : "Doanh nghiệp";
        dto.region = lead.getRegion() != null ? lead.getRegion() : "Toàn quốc";
        dto.employeeCount = lead.getEmployeeCount() != null ? lead.getEmployeeCount() : 0;
        dto.score = lead.getScore() != null ? lead.getScore() : 0;
        dto.status = lead.getStatus();

        if (lead.getAssignedAt() != null) {
            dto.assignedAt = lead.getAssignedAt().toInstant().toString();
        }

        int totalMin = lead.getSlaTotalMinutes() != null && lead.getSlaTotalMinutes() > 0 ? lead.getSlaTotalMinutes() : 120;
        dto.slaTotalMinutes = totalMin;

        if (lead.getSlaDeadline() != null) {
            dto.slaDeadline = lead.getSlaDeadline().getTime();
        } else if (lead.getAssignedAt() != null) {
            dto.slaDeadline = lead.getAssignedAt().getTime() + (totalMin * 60_000L);
        }

        if (lead.getAcceptedAt() != null) {
            dto.acceptedAt = lead.getAcceptedAt().toInstant().toString();
        }
        if (lead.getRejectedAt() != null) {
            dto.rejectedAt = lead.getRejectedAt().toInstant().toString();
        }

        dto.rejectReason = lead.getRejectReason();
        if (lead.getRejectReason() != null) {
            dto.rejectReasonLabel = REASON_LABELS.getOrDefault(lead.getRejectReason(), lead.getRejectReason());
        }
        dto.rejectDetails = lead.getRejectDetails();

        // Calculate real-time SLA flag if still in PENDING_RESPONSE
        if ("PENDING_RESPONSE".equals(lead.getStatus())) {
            long now = System.currentTimeMillis();
            if (dto.slaDeadline != null && now > dto.slaDeadline) {
                dto.slaResult = "OVERDUE";
            } else {
                dto.slaResult = "ON_TIME";
            }
        } else {
            dto.slaResult = lead.getSlaResult();
        }

        dto.responseTimeText = lead.getResponseTimeText();
        dto.contactingStage = lead.getContactingStage();

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
    public Integer getSlaTotalMinutes() { return slaTotalMinutes; }
    public void setSlaTotalMinutes(Integer slaTotalMinutes) { this.slaTotalMinutes = slaTotalMinutes; }
    public Long getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Long slaDeadline) { this.slaDeadline = slaDeadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(String acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(String rejectedAt) { this.rejectedAt = rejectedAt; }
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
}
