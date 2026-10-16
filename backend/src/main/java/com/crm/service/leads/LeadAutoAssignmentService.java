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

            if ("ROUND_ROBIN".equals(mechanism)) {
                // Round-Robin: pointer increments sequentially through reps list
                int currentIndex = Math.max(0, matchedRule.getLastAssignedIndex());
                int repIndex = currentIndex % repIds.size();
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

            // Fallback rep if lookup failed
            if (targetRep == null && !allReps.isEmpty()) {
                targetRep = allReps.get(0);
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
                        ruleDAO.updatePointerAndStats(conn, matchedRule.getId(), nextIndex, repUserId);
                        leadDAO.updateAssignment(conn, lead.getId(), repUserId, "ASSIGNED", null);
                        historyDAO.insert(conn, history);
                        conn.commit();

                        lead.setAssignedTo(repUserId);
                        lead.setAssignedToRepId(targetRep.getId());
                        lead.setStatus("ASSIGNED");
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
        lead.setAssignedTo(null);
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

        boolean regionMatch = false;
        if ("NATIONWIDE".equals(ruleRegion) || "ALL".equals(ruleRegion)) {
            regionMatch = true;
        } else if ("NORTH".equals(ruleRegion) && (leadRegion.toLowerCase().contains("bắc") || "NORTH".equals(leadRegionCode))) {
            regionMatch = true;
        } else if ("CENTRAL".equals(ruleRegion) && (leadRegion.toLowerCase().contains("trung") || "CENTRAL".equals(leadRegionCode))) {
            regionMatch = true;
        } else if ("SOUTH".equals(ruleRegion) && (leadRegion.toLowerCase().contains("nam") || "SOUTH".equals(leadRegionCode))) {
            regionMatch = true;
        } else if (ruleRegion.equalsIgnoreCase(leadRegionCode) || ruleRegion.equalsIgnoreCase(leadRegion)) {
            regionMatch = true;
        }

        if (!regionMatch) {
            return false;
        }

        // Check Industry criteria
        String ruleIndustry = rule.getIndustry() != null ? rule.getIndustry().trim() : "ALL";
        String leadIndustry = lead.getIndustry() != null ? lead.getIndustry().trim() : "";

        boolean industryMatch = false;
        if ("ALL".equalsIgnoreCase(ruleIndustry) || ruleIndustry.isEmpty()) {
            industryMatch = true;
        } else if (!leadIndustry.isEmpty() &&
                (leadIndustry.toLowerCase().contains(ruleIndustry.toLowerCase()) ||
                 ruleIndustry.toLowerCase().contains(leadIndustry.toLowerCase()))) {
            industryMatch = true;
        } else if (lead.getNotes() != null && lead.getNotes().toLowerCase().contains(ruleIndustry.toLowerCase())) {
            industryMatch = true;
        } else if (lead.getCompanyName() != null && lead.getCompanyName().toLowerCase().contains(ruleIndustry.toLowerCase())) {
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
            if (r.getId().equalsIgnoreCase(repId)) {
                targetRep = r;
                break;
            }
        }

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
                boolean updated = leadDAO.updateAssignment(conn, leadId, repUserId, "ASSIGNED", null);
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
            String digits = str.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
