package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.AssignmentRuleDAO;
import com.crm.dao.leads.LeadAssignmentHistoryDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.AssignmentRule;
import com.crm.model.leads.Lead;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class LeadAutoAssignmentService {

    private final AssignmentRuleDAO ruleDAO;
    private final LeadAssignmentHistoryDAO historyDAO;
    private final LeadDAO leadDAO;

    protected Connection open() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public LeadAutoAssignmentService() {
        this.ruleDAO = new AssignmentRuleDAO();
        this.historyDAO = new LeadAssignmentHistoryDAO();
        this.leadDAO = new LeadDAO();
    }

    public LeadAutoAssignmentService(AssignmentRuleDAO ruleDAO, LeadAssignmentHistoryDAO historyDAO, LeadDAO leadDAO) {
        this.ruleDAO = ruleDAO;
        this.historyDAO = historyDAO;
        this.leadDAO = leadDAO;
    }

    /**
     * Executes the Priority Rules Engine on a Lead (First-Match-Wins).
     *
     * @param lead the Lead to distribute
     * @return true if lead was assigned to a rep; false if it fell back to pending queue
     */
    public synchronized boolean assignLead(Lead lead) {
        if (lead == null || lead.getId() == null) {
            return false;
        }

        List<AssignmentRule> activeRules = ruleDAO.findActiveOrderedByPriority();
        List<SalesRepDTO> allReps = leadDAO.getSalesRepsWithWorkload();
        Map<String, SalesRepDTO> repLookup = new HashMap<>();
        for (SalesRepDTO r : allReps) {
            repLookup.put(r.getId(), r);
            Long userId = parseLongSafe(r.getId());
            if (userId != null) {
                repLookup.put(String.valueOf(userId), r);
                repLookup.put("USR-" + userId, r);
            }
        }

        AssignmentRule matchedRule = null;

        // 1. Evaluate rules by priority_order ASC (First-Match-Wins)
        for (AssignmentRule rule : activeRules) {
            if (isRuleMatch(rule, lead)) {
                matchedRule = rule;
                break;
            }
        }

        // 2. If matched a rule with designated reps
        if (matchedRule != null && matchedRule.getReps() != null && !matchedRule.getReps().isEmpty()) {
            List<String> repIds = matchedRule.getReps();
            SalesRepDTO targetRep = null;
            String mechanism = matchedRule.getMechanism() != null ? matchedRule.getMechanism().toUpperCase() : "ROUND_ROBIN";
            int nextIndex = matchedRule.getLastAssignedIndex();
            int repIndex = 0;

            if ("ROUND_ROBIN".equals(mechanism)) {
                // Round-Robin: pointer increments sequentially through reps list
                int currentIndex = Math.max(0, matchedRule.getLastAssignedIndex());
                repIndex = currentIndex % repIds.size();
                String targetId = repIds.get(repIndex);
                targetRep = repLookup.get(targetId);
                nextIndex = repIndex + 1;

            } else if ("LOAD_BALANCED".equals(mechanism)) {
                // Load-Balanced: rep with minimum current leads
                SalesRepDTO leastBusyRep = null;
                for (String repId : repIds) {
                    SalesRepDTO candidate = repLookup.get(repId);
                    if (candidate != null) {
                        if (leastBusyRep == null || candidate.getCurrentLeads() < leastBusyRep.getCurrentLeads()) {
                            leastBusyRep = candidate;
                        }
                    }
                }
                targetRep = leastBusyRep;

            } else {
                // DEDICATED
                String targetId = repIds.get(0);
                targetRep = repLookup.get(targetId);
            }

            if (targetRep == null && !allReps.isEmpty()) {
                int fallbackIndex = Math.max(0, repIndex) % allReps.size();
                targetRep = allReps.get(fallbackIndex);
            }

            if (targetRep != null) {
                Long repUserId = parseLongSafe(targetRep.getId());

                // Record assignment history
                AssignmentHistory history = new AssignmentHistory();
                history.setLeadId(lead.getId());
                history.setLeadName(lead.getFullName());
                history.setLeadPhone(lead.getPhone());
                history.setCompanyName(lead.getCompanyName());
                history.setRepId(targetRep.getId());
                history.setRepName(targetRep.getName());
                history.setRepDept(targetRep.getDept());
                history.setMethod("Tự động: Quy tắc #" + matchedRule.getPriority() + " (" + matchedRule.getName() + ")");
                history.setNote("Khớp quy tắc \"" + matchedRule.getName() + "\" theo cơ chế " + matchedRule.getMechanism() + ".");
                history.setAssignedAt(new Timestamp(System.currentTimeMillis()));

                // Atomic transaction: pointer + lead update + history
                try (Connection conn = open()) {
                    conn.setAutoCommit(false);
                    try {
                        if (!leadDAO.updateAssignment(conn, lead.getId(), repUserId, "PENDING_RESPONSE", null)) {
                            conn.rollback();
                            return false;
                        }
                        if (!ruleDAO.updatePointerAndStats(conn, matchedRule.getId(), nextIndex, repUserId))
                            throw new SQLException("Assignment rule no longer exists");
                        historyDAO.insert(conn, history);
                        conn.commit();

                        lead.setAssignedTo(repUserId);
                        lead.setAssignedToRepId(targetRep.getId());
                        lead.setAssignedToUserId(repUserId);
                        lead.setStatus("PENDING_RESPONSE");
                        return true;
                    } catch (Exception e) {
                        conn.rollback();
                        throw new RuntimeException("Lỗi giao dịch phân bổ tự động: " + e.getMessage(), e);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Lỗi kết nối CSDL khi phân bổ: " + e.getMessage(), e);
                }
            }
        }

        // 3. Fallback / Pending Queue: Lead did not match any active rules
        leadDAO.updateAssignment(lead.getId(), null, "PENDING_ASSIGNMENT", "Chưa khớp quy tắc phân bổ tự động nào");
        lead.setStatus("PENDING_ASSIGNMENT");
        lead.setAssignedTo((Long) null);
        lead.setUnassignedReason("Chưa khớp quy tắc phân bổ tự động nào");
        return false;
    }

    /**
     * Matches lead attributes against rule criteria (Region, Industry).
     */
    public boolean isRuleMatch(AssignmentRule rule, Lead lead) {
        if (rule == null || lead == null) return false;

        // Check Region criteria
        String ruleRegion = rule.getRegion() != null ? rule.getRegion().trim().toUpperCase() : "NATIONWIDE";
        String leadRegion = lead.getRegion() != null ? lead.getRegion().trim() : "";
        String leadRegionCode = lead.getRegionCode() != null ? lead.getRegionCode().trim().toUpperCase() : "";

        String ruleNameLower = rule.getName() != null ? rule.getName().toLowerCase() : "";
        String leadAllRegion = (leadRegion + " " + leadRegionCode + " "
                + (lead.getNotes() != null ? lead.getNotes() : "") + " "
                + (lead.getCompanyName() != null ? lead.getCompanyName() : "")).toLowerCase();

        boolean regionMatch = false;
        if ("NATIONWIDE".equals(ruleRegion) || "ALL".equals(ruleRegion) || ruleRegion.isEmpty()) {
            regionMatch = true;
        } else if (("NORTH".equals(ruleRegion) || "HANOI".equals(ruleRegion) || "HN".equals(ruleRegion) || ruleRegion.contains("HÀ NỘI") || ruleRegion.contains("BẮC") || ruleNameLower.contains("hà nội") || ruleNameLower.contains("miền bắc"))
                && (leadAllRegion.contains("bắc") || leadAllRegion.contains("hà nội") || leadAllRegion.contains("hanoi") || leadAllRegion.contains("hn") || leadAllRegion.contains("hải phòng") || "NORTH".equals(leadRegionCode) || "HANOI".equals(leadRegionCode))) {
            regionMatch = true;
        } else if (("CENTRAL".equals(ruleRegion) || ruleRegion.contains("TRUNG") || ruleNameLower.contains("miền trung") || ruleNameLower.contains("đà nẵng"))
                && (leadAllRegion.contains("trung") || leadAllRegion.contains("đà nẵng") || leadAllRegion.contains("huế") || "CENTRAL".equals(leadRegionCode))) {
            regionMatch = true;
        } else if (("SOUTH".equals(ruleRegion) || "HCM".equals(ruleRegion) || ruleRegion.contains("NAM") || ruleNameLower.contains("miền nam") || ruleNameLower.contains("hồ chí minh"))
                && (leadAllRegion.contains("nam") || leadAllRegion.contains("hồ chí minh") || leadAllRegion.contains("hcm") || leadAllRegion.contains("sài gòn") || leadAllRegion.contains("cần thơ") || "SOUTH".equals(leadRegionCode))) {
            regionMatch = true;
        } else if (ruleRegion.equalsIgnoreCase(leadRegionCode) || ruleRegion.equalsIgnoreCase(leadRegion) || leadAllRegion.contains(ruleRegion.toLowerCase())) {
            regionMatch = true;
        }

        if (!regionMatch) {
            return false;
        }

        // Check Industry criteria
        String ruleIndustry = rule.getIndustry() != null ? rule.getIndustry().trim() : "ALL";
        String leadIndustry = lead.getIndustry() != null ? lead.getIndustry().trim() : "";
        String leadIndLower = leadIndustry.toLowerCase();
        String ruleIndLower = ruleIndustry.toLowerCase();
        String leadAllInd = (leadIndustry + " "
                + (lead.getNotes() != null ? lead.getNotes() : "") + " "
                + (lead.getCompanyName() != null ? lead.getCompanyName() : "")).toLowerCase();

        boolean industryMatch = false;
        if ("ALL".equalsIgnoreCase(ruleIndustry) || ruleIndustry.isEmpty()) {
            industryMatch = true;
        } else if (("CNTT".equalsIgnoreCase(ruleIndustry) || ruleIndLower.contains("cntt") || ruleIndLower.contains("công nghệ") || ruleIndLower.contains("it") || ruleNameLower.contains("cntt")) && (
                leadAllInd.contains("cntt") ||
                leadAllInd.contains("công nghệ thông tin") ||
                leadAllInd.contains("it") ||
                leadAllInd.contains("viễn thông") ||
                leadAllInd.contains("phần mềm") ||
                leadIndLower.contains("cntt"))) {
            industryMatch = true;
        } else if (!leadIndustry.isEmpty() &&
                (leadIndLower.contains(ruleIndLower) ||
                 ruleIndLower.contains(leadIndLower))) {
            industryMatch = true;
        } else if (leadAllInd.contains(ruleIndLower)) {
            industryMatch = true;
        }

        return industryMatch;
    }

    /**
     * Batch processes all leads currently waiting in the Pending Assignment queue.
     *
     * @return number of leads successfully assigned
     */
    public int processPendingQueue() {
        List<Lead> pendingLeads = leadDAO.findUnassignedOrPendingQueue();
        if (pendingLeads.isEmpty()) {
            return 0;
        }

        int assignedCount = 0;
        for (Lead lead : pendingLeads) {
            boolean assigned = assignLead(lead);
            if (assigned) {
                assignedCount++;
            }
        }
        return assignedCount;
    }

    /**
     * Manually assigns a lead from the queue to a sales rep.
     */
    public boolean assignLeadManual(long leadId, String repId, String note) {
        Lead lead = leadDAO.findById(leadId);
        if (lead == null) {
            return false;
        }

        List<SalesRepDTO> allReps = leadDAO.getSalesRepsWithWorkload();
        SalesRepDTO targetRep = null;
        for (SalesRepDTO r : allReps) {
            if (Objects.equals(parseLongSafe(r.getId()), parseLongSafe(repId)) && parseLongSafe(repId) != null) {
                targetRep = r;
                break;
            }
        }

        if (targetRep == null) throw new IllegalArgumentException("Nhân viên phân bổ không tồn tại hoặc không có quyền Sales");

        String repName = targetRep != null ? targetRep.getName() : "Chuyên viên " + repId;
        String repDept = targetRep != null ? targetRep.getDept() : "Phòng Kinh doanh";
        Long repUserId = parseLongSafe(repId);

        AssignmentHistory history = new AssignmentHistory();
        history.setLeadId(leadId);
        history.setLeadName(lead.getFullName());
        history.setLeadPhone(lead.getPhone());
        history.setCompanyName(lead.getCompanyName());
        history.setRepId(repId);
        history.setRepName(repName);
        history.setRepDept(repDept);
        history.setMethod("Thủ công: Trưởng nhóm phân công");
        history.setNote(note != null && !note.isBlank() ? note : "Trưởng phòng chỉ định nhân sự chuyên trách.");
        history.setAssignedAt(new Timestamp(System.currentTimeMillis()));

        try (Connection conn = open()) {
            conn.setAutoCommit(false);
            try {
                boolean updated = leadDAO.updateAssignment(conn, leadId, repUserId, "PENDING_RESPONSE", null);
                if (!updated) {
                    conn.rollback();
                    return false;
                }
                historyDAO.insert(conn, history);
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi giao dịch phân bổ thủ công: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kết nối CSDL khi phân công thủ công: " + e.getMessage(), e);
        }
    }

    private Long parseLongSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            if (!str.matches("(?:USR-)?[1-9][0-9]*")) return null;
            return Long.parseLong(str.startsWith("USR-") ? str.substring(4) : str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
