package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.notifications.NotificationDAO;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to monitor Lead SLA deadlines and dispatch notifications to direct Team Leads.
 */
public class LeadSlaMonitorService {
    private static final Logger LOGGER = Logger.getLogger(LeadSlaMonitorService.class.getName());
    private final NotificationDAO notificationDAO;

    public LeadSlaMonitorService() {
        this(new NotificationDAO());
    }

    public LeadSlaMonitorService(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    /**
     * Checks all leads with status PENDING_RESPONSE that have exceeded sla_deadline.
     * Flags overdue leads as OVERDUE and sends in-app notifications to their respective Team Lead(s).
     * Deduplicates alerts using event_key so repeated scheduler executions do not spam recipients.
     *
     * @return count of new notifications dispatched
     */
    public int checkAndNotifyOverdueLeads() {
        return checkAndNotifyOverdueLeads(null);
    }

    public int checkAndNotifyOverdueLeads(Long specificLeadId) {
        int notificationsSent = 0;
        try (Connection c = DatabaseConfig.getConnection()) {
            // 1. Find leads currently pending response whose SLA deadline has passed
            String findOverdueSql = """
                SELECT l.id, l.full_name, l.assigned_to_user_id, l.sla_deadline,
                       rep.full_name AS rep_name
                FROM leads l
                LEFT JOIN users rep ON rep.id = l.assigned_to_user_id
                WHERE l.status = 'PENDING_RESPONSE'
                  AND l.sla_deadline IS NOT NULL
                  AND l.sla_deadline < NOW()
                """ + (specificLeadId != null ? " AND l.id = ?" : "");

            List<OverdueLeadInfo> overdueLeads = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(findOverdueSql)) {
                if (specificLeadId != null) {
                    ps.setLong(1, specificLeadId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String fullName = rs.getString("full_name");
                        Long repId = rs.getObject("assigned_to_user_id", Long.class);
                        Timestamp deadlineTs = rs.getTimestamp("sla_deadline");
                        String repName = rs.getString("rep_name");
                        overdueLeads.add(new OverdueLeadInfo(id, fullName, repId, deadlineTs, repName));
                    }
                }
            }

            if (overdueLeads.isEmpty()) {
                return 0;
            }

            // 2. Mark sla_result = 'OVERDUE' for overdue leads if not already set
            try (PreparedStatement updatePs = c.prepareStatement(
                    "UPDATE leads SET sla_result = 'OVERDUE' WHERE id = ? AND (sla_result IS NULL OR sla_result <> 'OVERDUE')")) {
                for (OverdueLeadInfo lead : overdueLeads) {
                    updatePs.setLong(1, lead.id);
                    updatePs.addBatch();
                }
                updatePs.executeBatch();
            }

            // 3. For each overdue lead, find their specific Team Lead(s) and dispatch alerts
            for (OverdueLeadInfo lead : overdueLeads) {
                List<Long> teamLeadIds = findTeamLeadUserIds(c, lead.id);

                long overdueMinutes = 0;
                if (lead.deadline != null) {
                    LocalDateTime dl = lead.deadline.toLocalDateTime();
                    overdueMinutes = Math.max(0, Duration.between(dl, LocalDateTime.now()).toMinutes());
                }

                for (Long teamLeadId : teamLeadIds) {
                    boolean created = notificationDAO.createLeadSlaAlert(
                            c, teamLeadId, lead.id, lead.fullName, lead.repName, overdueMinutes);
                    if (created) {
                        notificationsSent++;
                        LOGGER.info(String.format("Đã gửi cảnh báo quá hạn SLA Lead %d (%s) tới Trưởng nhóm userId=%d",
                                lead.id, lead.fullName, teamLeadId));
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Lỗi kiểm tra Lead SLA và gửi thông báo cho Trưởng nhóm", e);
        }
        return notificationsSent;
    }

    /**
     * Resolves the direct Team Lead(s) for a given Lead based on rep's team hierarchy.
     */
    public List<Long> findTeamLeadUserIds(Connection c, long leadId) throws SQLException {
        String findTeamLeadsSql = """
            SELECT DISTINCT tl.id
            FROM leads l
            JOIN users rep ON rep.id = COALESCE(l.assigned_to_user_id, l.owner_user_id)
            JOIN (
                WITH RECURSIVE ancestor_teams(id, parent_id, leader_id) AS (
                    SELECT t.id, t.parent_id, t.leader_id FROM teams t
                    UNION ALL
                    SELECT p.id, p.parent_id, p.leader_id FROM teams p
                    JOIN ancestor_teams a ON a.parent_id = p.id
                    WHERE p.active = TRUE
                )
                SELECT at.id AS team_id, at.leader_id
                FROM ancestor_teams at
            ) t_hier ON t_hier.team_id = rep.team_id
            JOIN users tl ON ((tl.id = t_hier.leader_id) OR (tl.team_id = t_hier.team_id))
            JOIN user_roles ur ON ur.user_id = tl.id
            JOIN roles r ON r.id = ur.role_id
            WHERE l.id = ? AND (r.code IN ('TEAM_LEAD', 'MANAGER') OR tl.id = t_hier.leader_id)
              AND tl.status = 'ACTIVE' AND tl.id != rep.id
            """;

        List<Long> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(findTeamLeadsSql)) {
            ps.setLong(1, leadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("id"));
                }
            }
        }
        return result;
    }

    private static class OverdueLeadInfo {
        final long id;
        final String fullName;
        final Long repId;
        final Timestamp deadline;
        final String repName;

        OverdueLeadInfo(long id, String fullName, Long repId, Timestamp deadline, String repName) {
            this.id = id;
            this.fullName = fullName;
            this.repId = repId;
            this.deadline = deadline;
            this.repName = repName;
        }
    }
}
