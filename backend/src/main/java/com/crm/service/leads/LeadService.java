package com.crm.service.leads;

import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.sql.Timestamp;
import java.util.*;

public class LeadService {
    private final LeadDAO leadDAO;
    private final DataScopeService dataScopeService;

    public LeadService() {
        this.leadDAO = new LeadDAO();
        this.dataScopeService = new DataScopeService();
    }

    public LeadService(LeadDAO leadDAO, DataScopeService dataScopeService) {
        this.leadDAO = leadDAO;
        this.dataScopeService = dataScopeService;
    }

    /**
     * S4-09 AC 1 & AC 2: Multi-criteria Lead search with SLA calculation and Data Scope.
     */
    public Map<String, Object> searchLeads(LeadFilterRequest filter, long currentUserId) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "lead", "read");

        int totalRecords = leadDAO.countWithFilters(filter, scope, currentUserId);
        List<Lead> items = leadDAO.findWithFilters(filter, scope, currentUserId);

        // Process SLA calculations for every lead (AC 2)
        long now = System.currentTimeMillis();
        for (Lead lead : items) {
            evaluateSla(lead, now);
        }

        int pageSize = filter.getSize();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("leads", items); // Alias for flexible consumers
        result.put("page", filter.getPage());
        result.put("size", pageSize);
        result.put("totalRecords", totalRecords);
        result.put("totalItems", totalRecords);
        result.put("totalPages", totalPages > 0 ? totalPages : 1);
        result.put("scope", scope != null ? scope.scopeType().name() : "ALL");
        result.put("stats", leadDAO.getLeadStats(scope, currentUserId));

        return result;
    }

    public Lead getLeadById(long id, long currentUserId) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "lead", "read");
        Lead lead = leadDAO.findById(id);
        if (lead == null) return null;

        if (scope != null && !scope.canAccessOwner(lead.getAssignedTo() != null ? lead.getAssignedTo() : 0)) {
            // Check if unassigned or allowed
            if (lead.getAssignedTo() != null && scope.scopeType() == com.crm.service.permissions.ScopeType.SELF 
                && lead.getAssignedTo() != currentUserId) {
                throw new SecurityException("Bạn không có quyền truy cập dữ liệu Lead này");
            }
        }

        evaluateSla(lead, System.currentTimeMillis());
        return lead;
    }

    public Lead createLead(Lead lead, long currentUserId) throws Exception {
        dataScopeService.resolve(currentUserId, "lead", "create");

        if (lead.getSlaDeadline() == null) {
            // Default SLA deadline: 120 minutes from creation
            long defaultSlaMin = lead.getSlaMinutesTotal() > 0 ? lead.getSlaMinutesTotal() : 120;
            lead.setSlaDeadline(new Timestamp(System.currentTimeMillis() + defaultSlaMin * 60 * 1000));
        }

        if (lead.getRating() == null || lead.getRating().isBlank()) {
            lead.setRating("WARM");
            lead.setTier("WARM");
        }

        long id = leadDAO.insert(lead);
        if (id > 0) {
            return getLeadById(id, currentUserId);
        }
        return null;
    }

    public boolean updateLead(Lead lead, long currentUserId) throws Exception {
        dataScopeService.resolve(currentUserId, "lead", "update");
        return leadDAO.update(lead);
    }

    public boolean deleteLead(long id, long currentUserId) throws Exception {
        dataScopeService.resolve(currentUserId, "lead", "delete");
        return leadDAO.delete(id);
    }

    public Map<String, Object> getLeadStats(long currentUserId) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "lead", "read");
        return leadDAO.getLeadStats(scope, currentUserId);
    }

    /**
     * S4-09 AC 2: SLA Overdue and Time-to-Contact evaluation engine.
     */
    public void evaluateSla(Lead lead, long now) {
        if (lead == null) return;

        boolean isClosed = "CONVERTED".equalsIgnoreCase(lead.getStatus()) || "LOST".equalsIgnoreCase(lead.getStatus());
        if (isClosed) {
            lead.setOverdueSla(false);
            lead.setSlaStatus("CLOSED");
            lead.setSlaRemainingText("Đã kết thúc");
            lead.setSlaRemainingMinutes(null);
            return;
        }

        Timestamp deadline = lead.getSlaDeadline();
        if (deadline == null && "NEW".equalsIgnoreCase(lead.getStatus()) && lead.getCreatedAt() != null) {
            // If no explicit deadline, compute 2 hours (120 minutes) from created_at
            long createdMs = lead.getCreatedAt().getTime();
            long totalMs = (lead.getSlaMinutesTotal() > 0 ? lead.getSlaMinutesTotal() : 120) * 60 * 1000L;
            deadline = new Timestamp(createdMs + totalMs);
            lead.setSlaDeadline(deadline);
        }

        if (deadline == null) {
            lead.setOverdueSla(false);
            lead.setSlaStatus("NONE");
            lead.setSlaRemainingText(null);
            lead.setSlaRemainingMinutes(null);
            return;
        }

        long diffMs = deadline.getTime() - now;
        if (diffMs < 0) {
            lead.setOverdueSla(true);
            lead.setSlaStatus("OVERDUE");
            long overdueMin = Math.abs(diffMs / (60 * 1000));
            lead.setSlaRemainingMinutes(-overdueMin);

            long hours = overdueMin / 60;
            long mins = overdueMin % 60;
            lead.setSlaRemainingText(hours > 0 
                ? String.format("Quá hạn %dh %dm", hours, mins) 
                : String.format("Quá hạn %d phút", mins));
        } else {
            lead.setOverdueSla(false);
            long leftMin = diffMs / (60 * 1000);
            lead.setSlaRemainingMinutes(leftMin);
            lead.setSlaStatus(leftMin < 30 ? "WARNING" : "SAFE");

            long hours = leftMin / 60;
            long mins = leftMin % 60;
            lead.setSlaRemainingText(hours > 0 
                ? String.format("Còn %dh %dm", hours, mins) 
                : String.format("Còn %d phút", mins));
        }
    }
}
