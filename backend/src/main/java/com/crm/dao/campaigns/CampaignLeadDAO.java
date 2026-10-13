package com.crm.dao.campaigns;

import com.crm.config.DatabaseConfig;
import com.crm.model.campaigns.CampaignLead;
import com.crm.model.campaigns.Lead;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CampaignLeadDAO {

    public List<CampaignLead> getLeadsByCampaign(long campaignId) throws SQLException {
        List<CampaignLead> list = new ArrayList<>();
        String sql = """
            SELECT cl.*, l.full_name AS lead_name, l.company_name, l.email, l.phone
            FROM campaign_leads cl
            JOIN leads l ON l.id = cl.lead_id
            WHERE cl.campaign_id = ? AND l.is_deleted = 0
            ORDER BY cl.created_at DESC
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, campaignId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CampaignLead item = new CampaignLead();
                    item.setCampaignId(rs.getLong("campaign_id"));
                    item.setLeadId(rs.getLong("lead_id"));
                    item.setLeadName(rs.getString("lead_name"));
                    item.setCompanyName(rs.getString("company_name"));
                    item.setEmail(rs.getString("email"));
                    item.setPhone(rs.getString("phone"));
                    item.setStatus(rs.getString("status"));
                    item.setNotes(rs.getString("notes"));

                    Timestamp ca = rs.getTimestamp("created_at");
                    if (ca != null) item.setCreatedAt(ca.toLocalDateTime());

                    Timestamp ua = rs.getTimestamp("updated_at");
                    if (ua != null) item.setUpdatedAt(ua.toLocalDateTime());

                    list.add(item);
                }
            }
        }
        return list;
    }

    public List<Lead> getAvailableLeads(long campaignId) throws SQLException {
        List<Lead> list = new ArrayList<>();
        String sql = """
            SELECT l.* FROM leads l
            WHERE l.is_deleted = 0
              AND l.id NOT IN (SELECT cl.lead_id FROM campaign_leads cl WHERE cl.campaign_id = ?)
            ORDER BY l.id DESC
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, campaignId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Lead lead = new Lead();
                    lead.setId(rs.getLong("id"));
                    lead.setFullName(rs.getString("full_name"));
                    lead.setCompanyName(rs.getString("company_name"));
                    lead.setEmail(rs.getString("email"));
                    lead.setPhone(rs.getString("phone"));
                    lead.setSource(rs.getString("source"));
                    lead.setStatus(rs.getString("status"));
                    list.add(lead);
                }
            }
        }
        return list;
    }

    public int addLeads(long campaignId, List<Long> leadIds, String status, String notes) throws SQLException {
        if (leadIds == null || leadIds.isEmpty()) return 0;

        String sql = """
            INSERT IGNORE INTO campaign_leads(campaign_id, lead_id, status, notes)
            VALUES (?, ?, ?, ?)
        """;
        int totalAdded = 0;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Long leadId : leadIds) {
                    ps.setLong(1, campaignId);
                    ps.setLong(2, leadId);
                    ps.setString(3, status != null && !status.isBlank() ? status : "SENT");
                    ps.setString(4, notes);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                conn.commit();
                for (int r : results) {
                    if (r > 0 || r == Statement.SUCCESS_NO_INFO) totalAdded++;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return totalAdded;
    }

    public boolean updateLeadStatus(long campaignId, long leadId, String status, String notes) throws SQLException {
        String sql = "UPDATE campaign_leads SET status = ?, notes = COALESCE(?, notes) WHERE campaign_id = ? AND lead_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, notes);
            ps.setLong(3, campaignId);
            ps.setLong(4, leadId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeLead(long campaignId, long leadId) throws SQLException {
        String sql = "DELETE FROM campaign_leads WHERE campaign_id = ? AND lead_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, campaignId);
            ps.setLong(2, leadId);
            return ps.executeUpdate() > 0;
        }
    }
}
