package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.AssignmentRule;
import com.crm.util.JsonUtil;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentRuleDAO {

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    public List<AssignmentRule> findAllOrderedByPriority() {
        String sql = "SELECT * FROM lead_assignment_rules ORDER BY priority_order ASC, id ASC";
        List<AssignmentRule> rules = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rules.add(mapResultSetToRule(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching assignment rules: " + e.getMessage());
            e.printStackTrace();
        }
        return rules;
    }

    public List<AssignmentRule> findActiveOrderedByPriority() {
        String sql = "SELECT * FROM lead_assignment_rules WHERE is_active = 1 ORDER BY priority_order ASC, id ASC";
        List<AssignmentRule> rules = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rules.add(mapResultSetToRule(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active assignment rules: " + e.getMessage());
            e.printStackTrace();
        }
        return rules;
    }

    public AssignmentRule findById(long id) {
        String sql = "SELECT * FROM lead_assignment_rules WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRule(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching assignment rule by id: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public long insert(AssignmentRule rule) {
        String sql = "INSERT INTO lead_assignment_rules (rule_code, name, priority_order, region, industry, " +
                "mechanism, reps, description, is_active, total_assigned, last_assigned_index, last_assigned_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String repsJson = JsonUtil.getGson().toJson(rule.getReps());

            ps.setString(1, rule.getRuleCode());
            ps.setString(2, rule.getName());
            ps.setInt(3, rule.getPriority());
            ps.setString(4, rule.getRegion() != null ? rule.getRegion() : "NATIONWIDE");
            ps.setString(5, rule.getIndustry() != null ? rule.getIndustry() : "ALL");
            ps.setString(6, rule.getMechanism() != null ? rule.getMechanism() : "ROUND_ROBIN");
            ps.setString(7, repsJson);
            ps.setString(8, rule.getDescription());
            ps.setBoolean(9, rule.getIsActive());
            ps.setInt(10, rule.getTotalAssigned());
            ps.setInt(11, rule.getLastAssignedIndex());
            if (rule.getLastAssignedUserId() != null) {
                ps.setLong(12, rule.getLastAssignedUserId());
            } else {
                ps.setNull(12, Types.BIGINT);
            }

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        rule.setId(generatedId);
                        if (rule.getRuleCode() == null || rule.getRuleCode().isBlank()) {
                            rule.setRuleCode("RULE-" + String.format("%02d", generatedId));
                            updateRuleCode(generatedId, rule.getRuleCode());
                        }
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting assignment rule: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean update(AssignmentRule rule) {
        String sql = "UPDATE lead_assignment_rules SET name = ?, priority_order = ?, region = ?, industry = ?, " +
                "mechanism = ?, reps = ?, description = ?, is_active = ?, total_assigned = ?, " +
                "last_assigned_index = ?, last_assigned_user_id = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String repsJson = JsonUtil.getGson().toJson(rule.getReps());

            ps.setString(1, rule.getName());
            ps.setInt(2, rule.getPriority());
            ps.setString(3, rule.getRegion());
            ps.setString(4, rule.getIndustry());
            ps.setString(5, rule.getMechanism());
            ps.setString(6, repsJson);
            ps.setString(7, rule.getDescription());
            ps.setBoolean(8, rule.getIsActive());
            ps.setInt(9, rule.getTotalAssigned());
            ps.setInt(10, rule.getLastAssignedIndex());
            if (rule.getLastAssignedUserId() != null) {
                ps.setLong(11, rule.getLastAssignedUserId());
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            ps.setLong(12, rule.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating assignment rule: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM lead_assignment_rules WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting assignment rule: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void updateRuleCode(long id, String ruleCode) {
        String sql = "UPDATE lead_assignment_rules SET rule_code = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruleCode);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating rule code: " + e.getMessage());
        }
    }

    public boolean updatePointerAndStats(Connection conn, long ruleId, int nextIndex, Long repUserId) throws SQLException {
        String sql = "UPDATE lead_assignment_rules SET last_assigned_index = ?, " +
                "total_assigned = total_assigned + 1, " +
                "last_assigned_user_id = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nextIndex);
            if (repUserId != null) {
                ps.setLong(2, repUserId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setLong(3, ruleId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePointerAndStats(long ruleId, int nextIndex, Long repUserId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return updatePointerAndStats(conn, ruleId, nextIndex, repUserId);
        } catch (SQLException e) {
            System.err.println("Error updating assignment pointer and stats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void batchSaveOrUpdate(List<AssignmentRule> rules) {
        if (rules == null || rules.isEmpty()) return;

        List<Long> keptIds = new ArrayList<>();
        for (AssignmentRule rule : rules) {
            if (rule.getId() != null && rule.getId() > 0) {
                AssignmentRule existing = findById(rule.getId());
                if (existing != null) {
                    update(rule);
                    keptIds.add(rule.getId());
                } else {
                    long newId = insert(rule);
                    if (newId > 0) keptIds.add(newId);
                }
            } else {
                long newId = insert(rule);
                if (newId > 0) keptIds.add(newId);
            }
        }

        List<AssignmentRule> allInDb = findAllOrderedByPriority();
        for (AssignmentRule r : allInDb) {
            if (r.getId() != null && !keptIds.contains(r.getId())) {
                delete(r.getId());
            }
        }
    }

    private AssignmentRule mapResultSetToRule(ResultSet rs) throws SQLException {
        AssignmentRule rule = new AssignmentRule();
        rule.setId(rs.getLong("id"));
        rule.setRuleCode(rs.getString("rule_code"));
        rule.setName(rs.getString("name"));
        rule.setPriority(rs.getInt("priority_order"));
        rule.setRegion(rs.getString("region"));
        rule.setIndustry(rs.getString("industry"));
        rule.setMechanism(rs.getString("mechanism"));

        String repsJson = rs.getString("reps");
        if (repsJson != null && !repsJson.isBlank()) {
            try {
                List<String> repsList = JsonUtil.getGson().fromJson(repsJson, STRING_LIST_TYPE);
                rule.setReps(repsList);
            } catch (Exception e) {
                rule.setReps(new ArrayList<>());
            }
        }

        rule.setDescription(rs.getString("description"));
        rule.setIsActive(rs.getBoolean("is_active"));
        rule.setTotalAssigned(rs.getInt("total_assigned"));
        rule.setLastAssignedIndex(rs.getInt("last_assigned_index"));

        long lastRepId = rs.getLong("last_assigned_user_id");
        if (!rs.wasNull()) {
            rule.setLastAssignedUserId(lastRepId);
        }

        rule.setCreatedAt(rs.getTimestamp("created_at"));
        rule.setUpdatedAt(rs.getTimestamp("updated_at"));
        return rule;
    }
}
