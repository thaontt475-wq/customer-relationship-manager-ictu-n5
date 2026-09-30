package com.crm.dao.scope;

import com.crm.service.scope.ScopeContext;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ScopedEntityDAO {

    public List<ScopeRecord> findVisible(
            Connection conn,
            ScopeEntityType type,
            ScopeContext actor,
            String search) throws SQLException {

        String scopeClause = DataScopeHelper.buildScopeClause("r.owner_user_id", "u", actor);

        String sql = "SELECT r.id, r." + type.labelColumn()
                + " AS label, r.owner_user_id, u.team_id AS owner_team_id "
                + "FROM " + type.tableName() + " r "
                + "JOIN users u ON u.id = r.owner_user_id "
                + "WHERE " + scopeClause + " "
                + "AND (? = '' OR r." + type.labelColumn()
                + " LIKE CONCAT('%', ?, '%')) "
                + "ORDER BY r.id";

        String normalizedSearch = search == null ? "" : search.trim();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = DataScopeHelper.bindScopeParameters(stmt, 1, actor);

            stmt.setString(index++, normalizedSearch);
            stmt.setString(index, normalizedSearch);

            List<ScopeRecord> items = new ArrayList<>();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long teamId = rs.getLong("owner_team_id");
                    Long ownerTeamId = rs.wasNull() ? null : teamId;

                    items.add(new ScopeRecord(
                            rs.getLong("id"),
                            rs.getString("label"),
                            rs.getLong("owner_user_id"),
                            ownerTeamId
                    ));
                }
            }

            return items;
        }
    }

    public long countVisible(
            Connection conn,
            ScopeEntityType type,
            ScopeContext actor,
            String search) throws SQLException {

        String scopeClause = DataScopeHelper.buildScopeClause("r.owner_user_id", "u", actor);

        String sql = "SELECT COUNT(*) FROM " + type.tableName() + " r "
                + "JOIN users u ON u.id = r.owner_user_id "
                + "WHERE " + scopeClause + " "
                + "AND (? = '' OR r." + type.labelColumn()
                + " LIKE CONCAT('%', ?, '%'))";

        String normalizedSearch = search == null ? "" : search.trim();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = DataScopeHelper.bindScopeParameters(stmt, 1, actor);

            stmt.setString(index++, normalizedSearch);
            stmt.setString(index, normalizedSearch);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public ScopeRecord findById(
            Connection conn,
            ScopeEntityType type,
            long id) throws SQLException {

        String sql = "SELECT r.id, r." + type.labelColumn()
                + " AS label, r.owner_user_id, u.team_id AS owner_team_id "
                + "FROM " + type.tableName() + " r "
                + "JOIN users u ON u.id = r.owner_user_id "
                + "WHERE r.id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long teamId = rs.getLong("owner_team_id");
                Long ownerTeamId = rs.wasNull() ? null : teamId;

                return new ScopeRecord(
                        rs.getLong("id"),
                        rs.getString("label"),
                        rs.getLong("owner_user_id"),
                        ownerTeamId
                );
            }
        }
    }

    public long insert(
            Connection conn,
            ScopeEntityType type,
            String label,
            long ownerUserId) throws SQLException {

        String sql = "INSERT INTO " + type.tableName() + " (" + type.labelColumn() + ", owner_user_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, label != null ? label.trim() : "");
            stmt.setLong(2, ownerUserId);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }
        return 0;
    }

    public int update(
            Connection conn,
            ScopeEntityType type,
            long id,
            String label) throws SQLException {

        String sql = "UPDATE " + type.tableName() + " SET " + type.labelColumn() + " = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, label != null ? label.trim() : "");
            stmt.setLong(2, id);
            return stmt.executeUpdate();
        }
    }

    public int delete(
            Connection conn,
            ScopeEntityType type,
            long id) throws SQLException {

        String sql = "DELETE FROM " + type.tableName() + " WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate();
        }
    }
}