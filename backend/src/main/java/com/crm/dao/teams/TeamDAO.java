package com.crm.dao.teams;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class TeamDAO {

    private static volatile boolean schemaChecked = false;

    public void ensureSchema() {
        if (schemaChecked) {
            return;
        }
        synchronized (TeamDAO.class) {
            if (schemaChecked) {
                return;
            }
            try (Connection conn = DatabaseConfig.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                String catalog = conn.getCatalog();

                // Check leader_id
                boolean hasLeader = false;
                try (ResultSet rs = meta.getColumns(catalog, null, "teams", "leader_id")) {
                    if (rs.next()) {
                        hasLeader = true;
                    }
                }
                if (!hasLeader) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE teams ADD COLUMN leader_id BIGINT NULL");
                    } catch (Exception ignored) {
                    }
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE teams ADD CONSTRAINT fk_teams_leader FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE SET NULL");
                    } catch (Exception ignored) {
                    }
                }

                // Check region_id
                boolean hasRegion = false;
                try (ResultSet rs = meta.getColumns(catalog, null, "teams", "region_id")) {
                    if (rs.next()) {
                        hasRegion = true;
                    }
                }
                if (!hasRegion) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE teams ADD COLUMN region_id BIGINT NULL");
                    } catch (Exception ignored) {
                    }
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE teams ADD CONSTRAINT fk_teams_region FOREIGN KEY (region_id) REFERENCES master_data(id) ON DELETE SET NULL");
                    } catch (Exception ignored) {
                    }
                }

                // Ensure regions in master_data
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("""
                            INSERT IGNORE INTO master_data (type, code, name, active, display_order) VALUES
                            ('region', 'MB', 'Miền Bắc', 1, 1),
                            ('region', 'MT', 'Miền Trung', 1, 2),
                            ('region', 'MN', 'Miền Nam', 1, 3),
                            ('region', 'TN', 'Tây Nguyên', 1, 4),
                            ('region', 'MDB', 'Miền Đông Nam Bộ', 1, 5),
                            ('region', 'MTW', 'Đồng bằng Sông Cửu Long (Miền Tây)', 1, 6)
                            """);
                } catch (Exception ignored) {
                }

                // Set default region for sample teams
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("""
                            UPDATE teams t
                            JOIN master_data md ON md.type = 'region' AND md.code = 'MB'
                            SET t.region_id = md.id
                            WHERE t.name LIKE '%Bắc%' AND t.region_id IS NULL
                            """);
                    stmt.executeUpdate("""
                            UPDATE teams t
                            JOIN master_data md ON md.type = 'region' AND md.code = 'MT'
                            SET t.region_id = md.id
                            WHERE t.name LIKE '%Trung%' AND t.region_id IS NULL
                            """);
                    stmt.executeUpdate("""
                            UPDATE teams t
                            JOIN master_data md ON md.type = 'region' AND md.code = 'MN'
                            SET t.region_id = md.id
                            WHERE t.name LIKE '%Nam%' AND t.region_id IS NULL
                            """);

                    // Fix roles encoding if needed
                    stmt.executeUpdate("UPDATE roles SET name = 'Quản trị viên' WHERE code = 'ADMIN'");
                    stmt.executeUpdate("UPDATE roles SET name = 'Giám đốc kinh doanh' WHERE code = 'DIRECTOR'");
                    stmt.executeUpdate("UPDATE roles SET name = 'Trưởng nhóm kinh doanh' WHERE code = 'TEAM_LEAD'");
                    stmt.executeUpdate("UPDATE roles SET name = 'Nhân viên kinh doanh' WHERE code = 'SALES_REP' OR code = 'SALE'");
                    stmt.executeUpdate("UPDATE roles SET name = 'Nhân viên Marketing' WHERE code = 'MARKETING'");
                    stmt.executeUpdate("UPDATE roles SET name = 'Chăm sóc khách hàng' WHERE code = 'CUSTOMER_SUCCESS'");
                    stmt.executeUpdate("UPDATE roles SET name = 'Kế toán' WHERE code = 'ACCOUNTANT'");
                } catch (Exception ignored) {
                }

                schemaChecked = true;
            } catch (Exception e) {
                // Ignore schema auto-check error if DB is read-only or already configured
                schemaChecked = true;
            }
        }
    }

    public List<Map<String, Object>> findAll(
            String keyword,
            Boolean active
    ) throws SQLException {
        return findAll(keyword, active, null);
    }

    public List<Map<String, Object>> findAll(
            String keyword,
            Boolean active,
            Long regionId
    ) throws SQLException {
        ensureSchema();

        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.id,
                    t.name,
                    t.active,
                    t.parent_id,
                    p.name AS parent_name,
                    t.leader_id,
                    u.full_name AS leader_name,
                    u.email AS leader_email,
                    t.region_id,
                    md.code AS region_code,
                    md.name AS region_name,
                    COUNT(DISTINCT m.id) AS member_count,
                    t.created_at,
                    t.updated_at
                FROM teams t
                LEFT JOIN teams p ON p.id = t.parent_id
                LEFT JOIN users u ON u.id = t.leader_id
                LEFT JOIN master_data md ON md.id = t.region_id
                LEFT JOIN users m ON m.team_id = t.id AND m.status = 'ACTIVE'
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND LOWER(t.name) LIKE ?");
            params.add("%" + keyword.trim().toLowerCase() + "%");
        }

        if (active != null) {
            sql.append(" AND t.active = ?");
            params.add(active);
        }

        if (regionId != null) {
            sql.append(" AND t.region_id = ?");
            params.add(regionId);
        }

        sql.append("""
                 GROUP BY
                    t.id,
                    t.name,
                    t.active,
                    t.parent_id,
                    p.name,
                    t.leader_id,
                    u.full_name,
                    u.email,
                    t.region_id,
                    md.code,
                    md.name,
                    t.created_at,
                    t.updated_at
                 ORDER BY t.name
                """);

        List<Map<String, Object>> result = new ArrayList<>();

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    public Map<String, Object> findById(long id) throws SQLException {
        ensureSchema();

        String sql = """
                SELECT
                    t.id,
                    t.name,
                    t.active,
                    t.parent_id,
                    p.name AS parent_name,
                    t.leader_id,
                    u.full_name AS leader_name,
                    u.email AS leader_email,
                    t.region_id,
                    md.code AS region_code,
                    md.name AS region_name,
                    COUNT(DISTINCT m.id) AS member_count,
                    t.created_at,
                    t.updated_at
                FROM teams t
                LEFT JOIN teams p ON p.id = t.parent_id
                LEFT JOIN users u ON u.id = t.leader_id
                LEFT JOIN master_data md ON md.id = t.region_id
                LEFT JOIN users m ON m.team_id = t.id AND m.status = 'ACTIVE'
                WHERE t.id = ?
                GROUP BY
                    t.id,
                    t.name,
                    t.active,
                    t.parent_id,
                    p.name,
                    t.leader_id,
                    u.full_name,
                    u.email,
                    t.region_id,
                    md.code,
                    md.name,
                    t.created_at,
                    t.updated_at
                """;

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    public long create(
            String name,
            Long parentId,
            Long leaderId,
            Long regionId,
            boolean active
    ) throws SQLException {
        ensureSchema();

        String sql = """
                INSERT INTO teams(name, parent_id, leader_id, region_id, active)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, name.trim());
            setNullableLong(statement, 2, parentId);
            setNullableLong(statement, 3, leaderId);
            setNullableLong(statement, 4, regionId);
            statement.setBoolean(5, active);

            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Không lấy được id nhóm vừa tạo");
    }

    public void update(
            long id,
            String name,
            Long parentId,
            Long leaderId,
            Long regionId,
            boolean active
    ) throws SQLException {
        ensureSchema();

        String sql = """
                UPDATE teams
                SET name = ?,
                    parent_id = ?,
                    leader_id = ?,
                    region_id = ?,
                    active = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, name.trim());
            setNullableLong(statement, 2, parentId);
            setNullableLong(statement, 3, leaderId);
            setNullableLong(statement, 4, regionId);
            statement.setBoolean(5, active);
            statement.setLong(6, id);

            statement.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM teams WHERE id = ?";

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public boolean hasChildren(long id) throws SQLException {
        String sql = "SELECT 1 FROM teams WHERE parent_id = ? LIMIT 1";

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hasMembers(long id) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE team_id = ? AND status <> 'DELETED' LIMIT 1";

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Map<String, Object>> getMembers(long teamId) throws SQLException {
        String sql = """
                SELECT
                    u.id,
                    u.full_name,
                    u.email,
                    u.status,
                    GROUP_CONCAT(r.code SEPARATOR ', ') AS role_codes,
                    GROUP_CONCAT(r.name SEPARATOR ', ') AS role_names
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE u.team_id = ? AND u.status <> 'DELETED'
                GROUP BY u.id, u.full_name, u.email, u.status
                ORDER BY u.full_name
                """;

        List<Map<String, Object>> members = new ArrayList<>();

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, teamId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> member = new LinkedHashMap<>();
                    member.put("id", rs.getLong("id"));
                    member.put("fullName", rs.getString("full_name"));
                    member.put("email", rs.getString("email"));
                    member.put("status", rs.getString("status"));
                    member.put("roleCodes", rs.getString("role_codes"));
                    member.put("roleNames", rs.getString("role_names"));
                    members.add(member);
                }
            }
        }

        return members;
    }

    public Map<String, Object> getLookups() throws SQLException {
        ensureSchema();
        Map<String, Object> lookups = new LinkedHashMap<>();

        List<Map<String, Object>> regions = new ArrayList<>();
        String regionSql = """
                SELECT id, code, name
                FROM master_data
                WHERE type = 'region' AND active = TRUE
                ORDER BY display_order ASC, id ASC
                """;
        try (
                Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(regionSql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", rs.getLong("id"));
                r.put("code", rs.getString("code"));
                r.put("name", rs.getString("name"));
                regions.add(r);
            }
        }
        lookups.put("regions", regions);

        List<Map<String, Object>> users = new ArrayList<>();
        String userSql = """
                SELECT id, full_name, email
                FROM users
                WHERE status = 'ACTIVE'
                ORDER BY full_name ASC
                """;
        try (
                Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(userSql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("id", rs.getLong("id"));
                u.put("fullName", rs.getString("full_name"));
                u.put("email", rs.getString("email"));
                users.add(u);
            }
        }
        lookups.put("users", users);

        return lookups;
    }

    public boolean existsName(String name, Long excludeId) throws SQLException {
        String sql = excludeId == null
                ? "SELECT 1 FROM teams WHERE LOWER(name) = LOWER(?) LIMIT 1"
                : "SELECT 1 FROM teams WHERE LOWER(name) = LOWER(?) AND id <> ? LIMIT 1";

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, name.trim());
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsActive(long id) throws SQLException {
        String sql = """
                SELECT 1
                FROM teams
                WHERE id = ?
                  AND active = TRUE
                """;

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean createsCycle(long unitId, long parentId) throws SQLException {
        Long current = parentId;
        Set<Long> visited = new HashSet<>();

        while (current != null) {
            if (current == unitId) {
                return true;
            }
            if (!visited.add(current)) {
                return true;
            }

            String sql = "SELECT parent_id FROM teams WHERE id = ?";
            try (
                    Connection connection = DatabaseConfig.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)
            ) {
                statement.setLong(1, current);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        current = rs.getObject("parent_id", Long.class);
                    } else {
                        return false;
                    }
                }
            }
        }

        return false;
    }

    private void setNullableLong(
            PreparedStatement statement,
            int index,
            Long value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private Map<String, Object> map(ResultSet rs) throws SQLException {
        Map<String, Object> team = new LinkedHashMap<>();

        team.put("id", rs.getLong("id"));
        team.put("name", rs.getString("name"));
        team.put("active", rs.getBoolean("active"));

        Long parentId = rs.getObject("parent_id", Long.class);
        team.put("parentId", parentId);
        team.put("parentName", rs.getString("parent_name"));

        Long leaderId = rs.getObject("leader_id", Long.class);
        team.put("leaderId", leaderId);
        team.put("leaderName", rs.getString("leader_name"));
        team.put("leaderEmail", rs.getString("leader_email"));

        Long regionId = rs.getObject("region_id", Long.class);
        team.put("regionId", regionId);
        team.put("regionCode", rs.getString("region_code"));
        team.put("regionName", rs.getString("region_name"));

        team.put("memberCount", rs.getInt("member_count"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        team.put("createdAt", createdAt != null ? createdAt.toString() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        team.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);

        return team;
    }
}