package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadAssignmentHistoryDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.LeadSlaResponseDTO;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class LeadSlaService {

    private final LeadDAO leadDAO;
    private final LeadAssignmentHistoryDAO historyDAO;
    private final DataScopeService scopes;

    public LeadSlaService() {
        this(new LeadDAO(), new LeadAssignmentHistoryDAO(), new DataScopeService());
    }

    public LeadSlaService(LeadDAO leadDAO, LeadAssignmentHistoryDAO historyDAO, DataScopeService scopes) {
        this.leadDAO = leadDAO;
        this.historyDAO = historyDAO;
        this.scopes = scopes;
    }

    public List<LeadSlaResponseDTO> getMyAssignedLeads(long currentUserId) throws SQLException {
        DataScopeContext scope = scopes.resolve(currentUserId, "lead", "read");
        List<Lead> leads = leadDAO.findMyAssignedLeads(currentUserId, scope);
        List<LeadSlaResponseDTO> dtoList = new ArrayList<>();
        for (Lead lead : leads) {
            dtoList.add(LeadSlaResponseDTO.fromModel(lead));
        }
        return dtoList;
    }

    public LeadSlaResponseDTO acceptLead(long currentUserId, long leadId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "lead", "accept");

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Lead lead = leadDAO.findById(conn, leadId, true);
                if (lead == null) {
                    throw new NoSuchElementException("Không tìm thấy Lead với ID: " + leadId);
                }

                if (lead.isLocked() || "CONVERTED".equals(lead.getStatus())) {
                    throw new IllegalStateException("Lead đã bị khóa hoặc đã chuyển đổi, không thể tiếp nhận.");
                }

                // Check authorization and data scope
                if (lead.getAssignedToUserId() != null && lead.getAssignedToUserId() != currentUserId) {
                    if (!scope.canAccessOwner(lead.getAssignedToUserId())) {
                        throw new SecurityException("Bạn không có quyền tiếp nhận Lead này do không thuộc phạm vi sở hữu.");
                    }
                }

                Timestamp now = new Timestamp(System.currentTimeMillis());
                Timestamp deadline = lead.getSlaDeadline();
                if (deadline == null && lead.getAssignedAt() != null) {
                    int totalMin = lead.getSlaTotalMinutes() != null && lead.getSlaTotalMinutes() > 0 ? lead.getSlaTotalMinutes() : 120;
                    deadline = new Timestamp(lead.getAssignedAt().getTime() + (totalMin * 60_000L));
                }

                boolean wasOnTime = deadline == null || !now.after(deadline);
                String slaResult = wasOnTime ? "ON_TIME" : "OVERDUE";

                long diffMinutes = lead.getAssignedAt() != null ? Math.max(1, (now.getTime() - lead.getAssignedAt().getTime()) / 60_000L) : 0;
                String responseTimeText = wasOnTime
                    ? "Tiếp nhận đúng hạn (sau " + diffMinutes + " phút)"
                    : "Tiếp nhận trễ hạn (sau " + diffMinutes + " phút)";
                String contactingStage = "Mới tiếp nhận - Sẵn sàng gọi điện tư vấn";

                lead.setStatus("CONTACTING");
                lead.setAcceptedAt(now);
                lead.setSlaResult(slaResult);
                lead.setResponseTimeText(responseTimeText);
                lead.setContactingStage(contactingStage);

                leadDAO.updateAccept(conn, lead);

                // Insert into audit history
                AssignmentHistory history = new AssignmentHistory();
                history.setLeadId(lead.getId());
                history.setLeadName(lead.getFullName());
                history.setLeadPhone(lead.getPhone());
                history.setCompanyName(lead.getCompanyName());
                history.setRepId("USR-" + currentUserId);
                history.setRepName("Chuyên viên #" + currentUserId);
                history.setRepDept("Phòng Kinh doanh");
                history.setMethod("Tiếp nhận Lead SLA");
                history.setActionType("ACCEPT");
                history.setNote(responseTimeText);
                history.setAssignedAt(now);

                historyDAO.insert(conn, history);

                conn.commit();
                return LeadSlaResponseDTO.fromModel(lead);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public LeadSlaResponseDTO rejectLead(long currentUserId, long leadId, String reason, String details) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "lead", "reject");

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối không được để trống.");
        }
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Chi tiết giải trình từ chối không được để trống.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Lead lead = leadDAO.findById(conn, leadId, true);
                if (lead == null) {
                    throw new NoSuchElementException("Không tìm thấy Lead với ID: " + leadId);
                }

                if (lead.isLocked() || "CONVERTED".equals(lead.getStatus())) {
                    throw new IllegalStateException("Lead đã bị khóa hoặc đã chuyển đổi, không thể từ chối.");
                }

                // Check authorization and data scope
                if (lead.getAssignedToUserId() != null && lead.getAssignedToUserId() != currentUserId) {
                    if (!scope.canAccessOwner(lead.getAssignedToUserId())) {
                        throw new SecurityException("Bạn không có quyền từ chối Lead này do không thuộc phạm vi sở hữu.");
                    }
                }

                Timestamp now = new Timestamp(System.currentTimeMillis());

                // AC2: Lead returns to pending assignment queue
                lead.setStatus("PENDING_ASSIGNMENT");
                lead.setAssignedTo((Long) null);
                lead.setAssignedToUserId(null);
                lead.setRejectedAt(now);
                lead.setRejectReason(reason);
                lead.setRejectDetails(details);
                lead.setUnassignedReason("Bị từ chối bởi nhân viên #" + currentUserId + " (" + reason + ": " + details + ")");

                leadDAO.updateReject(conn, lead);

                // Insert into audit history
                AssignmentHistory history = new AssignmentHistory();
                history.setLeadId(lead.getId());
                history.setLeadName(lead.getFullName());
                history.setLeadPhone(lead.getPhone());
                history.setCompanyName(lead.getCompanyName());
                history.setRepId("USR-" + currentUserId);
                history.setRepName("Chuyên viên #" + currentUserId);
                history.setRepDept("Phòng Kinh doanh");
                history.setMethod("Từ chối Lead SLA");
                history.setActionType("REJECT");
                history.setNote("Lý do: " + reason + " | Chi tiết: " + details);
                history.setAssignedAt(now);

                historyDAO.insert(conn, history);

                conn.commit();
                return LeadSlaResponseDTO.fromModel(lead);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
