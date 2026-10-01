package com.crm.dao.organization;

import com.crm.model.Organization;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrganizationDAO {

    private static final String UNIT_COLUMNS = "SELECT t.id, t.name, t.parent_id, "
            + "t.leader_user_id, t.region, t.active, "
            + "COALESCE(NULLIF(leader.display_name, ''), NULLIF(leader.full_name, ''), "
            + "leader.username) AS manager_name, "
            + "(SELECT GROUP_CONCAT(DISTINCT r.name ORDER BY r.name SEPARATOR ', ') "
            + " FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
            + " WHERE ur.user_id = t.leader_user_id) AS manager_role "
            + "FROM teams t LEFT JOIN users leader ON leader.id = t.leader_user_id ";

    public List<Organization> findAll(Connection conn) throws SQLException {
        String sql = UNIT_COLUMNS + "ORDER BY t.name, t.id";
        Map<Long, Organization> unitsById = new LinkedHashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Organization unit = mapUnit(rs);
                unitsById.put(unit.getId(), unit);
            }
        }

        if (!unitsById.isEmpty()) {
            loadMembers(conn, unitsById, null);
        }
        return new ArrayList<>(unitsById.values());
    }

    public Organization findById(Connection conn, long unitId) throws SQLException {
        String sql = UNIT_COLUMNS + "WHERE t.id = ?";
        Organization unit;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, unitId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                unit = mapUnit(rs);
            }
        }

        Map<Long, Organization> unitsById = new LinkedHashMap<>();
        unitsById.put(unit.getId(), unit);
        loadMembers(conn, unitsById, unitId);
        return unit;
    }

    public Organization findByIdForUpdate(Connection conn, long unitId) throws SQLException {
        String sql = "SELECT id, name, parent_id, leader_user_id, region, active, "
                + "NULL AS manager_name, NULL AS manager_role "
                + "FROM teams WHERE id = ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, unitId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapUnit(rs) : null;
            }
        }
    }

    public boolean existsByNameExcluding(Connection conn, String name, Long excludedId)
            throws SQLException {
        String sql = "SELECT 1 FROM teams WHERE LOWER(name) = LOWER(?)"
                + (excludedId == null ? "" : " AND id <> ?")
                + " LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            if (excludedId != null) {
                stmt.setLong(2, excludedId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Long findUnitIdByLeader(Connection conn, long leaderUserId) throws SQLException {
        String sql = "SELECT id FROM teams WHERE leader_user_id = ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, leaderUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    public long insert(Connection conn, String name, Long parentId, long leaderUserId,
                       String region, boolean active) throws SQLException {
        String sql = "INSERT INTO teams (name, parent_id, leader_user_id, region, active) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name.trim());
            setNullableLong(stmt, 2, parentId);
            stmt.setLong(3, leaderUserId);
            stmt.setString(4, region);
            stmt.setBoolean(5, active);

            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
        }
        return 0;
    }

    public int update(Connection conn, long unitId, String name, Long parentId,
                      long leaderUserId, String region, boolean active) throws SQLException {
        String sql = "UPDATE teams SET name = ?, parent_id = ?, leader_user_id = ?, "
                + "region = ?, active = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            setNullableLong(stmt, 2, parentId);
            stmt.setLong(3, leaderUserId);
            stmt.setString(4, region);
            stmt.setBoolean(5, active);
            stmt.setLong(6, unitId);
            return stmt.executeUpdate();
        }
    }

    private void loadMembers(Connection conn, Map<Long, Organization> unitsById, Long unitId)
            throws SQLException {
        String sql = "SELECT u.id, u.team_id, "
                + "COALESCE(NULLIF(u.display_name, ''), NULLIF(u.full_name, ''), u.username) "
                + "AS member_name, u.email, "
                + "(SELECT GROUP_CONCAT(DISTINCT r.name ORDER BY r.name SEPARATOR ', ') "
                + " FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + " WHERE ur.user_id = u.id) AS role_name, "
                + "DATE_FORMAT(u.created_at, '%Y-%m-%d') AS joined_date "
                + "FROM users u WHERE u.team_id IS NOT NULL"
                + (unitId == null ? "" : " AND u.team_id = ?")
                + " ORDER BY member_name, u.id";

        Map<Long, List<Organization.Member>> membersByUnitId = new LinkedHashMap<>();
        for (Long id : unitsById.keySet()) {
            membersByUnitId.put(id, new ArrayList<>());
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (unitId != null) {
                stmt.setLong(1, unitId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    List<Organization.Member> members = membersByUnitId.get(rs.getLong("team_id"));
                    if (members != null) {
                        members.add(mapMember(rs));
                    }
                }
            }
        }

        for (Map.Entry<Long, Organization> entry : unitsById.entrySet()) {
            List<Organization.Member> members = membersByUnitId.get(entry.getKey());
            entry.getValue().setMembers(members);
            entry.getValue().setMemberCount(members.size());
        }
    }

    private Organization mapUnit(ResultSet rs) throws SQLException {
        Organization unit = new Organization();
        unit.setId(rs.getLong("id"));
        unit.setName(rs.getString("name"));

        long parentId = rs.getLong("parent_id");
        unit.setParentId(rs.wasNull() ? null : parentId);

        long managerId = rs.getLong("leader_user_id");
        unit.setManagerId(rs.wasNull() ? null : managerId);
        unit.setManagerName(rs.getString("manager_name"));
        unit.setManagerRole(rs.getString("manager_role"));
        unit.setRegion(rs.getString("region"));
        unit.setActive(rs.getBoolean("active"));
        unit.setMembers(new ArrayList<>());
        unit.setMemberCount(0);
        return unit;
    }

    private Organization.Member mapMember(ResultSet rs) throws SQLException {
        Organization.Member member = new Organization.Member();
        member.setId(rs.getLong("id"));
        member.setName(rs.getString("member_name"));
        member.setEmail(rs.getString("email"));
        member.setRole(rs.getString("role_name"));
        member.setJoinedDate(rs.getString("joined_date"));
        return member;
    }

    private void setNullableLong(PreparedStatement stmt, int index, Long value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.BIGINT);
        } else {
            stmt.setLong(index, value);
        }
    }
}
