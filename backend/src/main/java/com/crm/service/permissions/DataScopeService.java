package com.crm.service.permissions;

import com.crm.config.DatabaseConfig;
import java.sql.*;
import java.util.*;

/** Resolves action-specific scope from database roles, never from request parameters. */
public class DataScopeService {
    public DataScopeContext resolve(long currentUserId, String module, String action) throws SQLException {
        if (!Set.of("customer", "opportunity", "activity", "quote").contains(module)) {
            throw new IllegalArgumentException("Module phạm vi dữ liệu không hợp lệ");
        }
        String permission = module + "." + action;
        try (Connection connection = DatabaseConfig.getConnection()) {
            Long teamId;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT team_id FROM users WHERE id = ? AND status = 'ACTIVE'")) {
                statement.setLong(1, currentUserId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) throw new SecurityException("Phiên đăng nhập không hợp lệ");
                    teamId = rs.getObject(1, Long.class);
                }
            }

            ScopeType effective = null;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT rms.scope_type FROM user_roles ur
                    JOIN role_module_scopes rms ON rms.role_id = ur.role_id
                    JOIN role_permissions rp ON rp.role_id = ur.role_id
                    JOIN permissions p ON p.id = rp.permission_id
                    WHERE ur.user_id = ? AND rms.module_code = ? AND p.code = ?
                    """)) {
                statement.setLong(1, currentUserId);
                statement.setString(2, module);
                statement.setString(3, permission);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        ScopeType candidate = ScopeType.valueOf(rs.getString(1));
                        effective = effective == null ? candidate : ScopeType.widest(effective, candidate);
                    }
                }
            }
            if (effective == null) throw new SecurityException("Bạn không có quyền truy cập dữ liệu này.");
            if (effective == ScopeType.ALL) {
                return new DataScopeContext(currentUserId, module, effective, teamId, Set.of());
            }
            if (effective == ScopeType.SELF) {
                return new DataScopeContext(currentUserId, module, effective, teamId, Set.of(currentUserId));
            }
            if (teamId == null) throw new SecurityException("Trưởng nhóm kinh doanh phải được gán vào một nhóm.");

            Set<Long> owners = new LinkedHashSet<>();
            owners.add(currentUserId);
            try (PreparedStatement statement = connection.prepareStatement("""
                    WITH RECURSIVE descendant_teams(id) AS (
                        SELECT id FROM teams WHERE id = ? AND active = TRUE
                        UNION DISTINCT
                        SELECT child.id FROM teams child
                        JOIN descendant_teams parent ON child.parent_id = parent.id
                        WHERE child.active = TRUE
                    )
                    SELECT u.id FROM users u
                    JOIN descendant_teams dt ON dt.id = u.team_id
                    WHERE u.status = 'ACTIVE'
                    """)) {
                statement.setLong(1, teamId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) owners.add(rs.getLong(1));
                }
            }
            return new DataScopeContext(currentUserId, module, effective, teamId, owners);
        }
    }
}
