package com.crm.dao.scope;

import com.crm.service.scope.ScopeContext;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScopedEntityDAO {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public List<ScopeRecord> findVisible(
            Connection conn,
            ScopeEntityType type,
            ScopeContext actor,
            String search) throws SQLException {

        String scope = actor.dataScope() == null
                ? "SELF"
                : actor.dataScope().trim().toUpperCase(Locale.ROOT);

        String scopeClause;
        switch (scope) {
            case "ALL" -> scopeClause = "1=1";
            case "TEAM" -> scopeClause =
                    actor.teamId() == null ? "r.owner_user_id = ?" : "(u.team_id = ? OR r.owner_user_id = ?)";
            default -> scopeClause = "r.owner_user_id = ?";
        }

        String sql = "SELECT r.id, r." + type.labelColumn() + " AS label, "
                + "r.owner_user_id, u.team_id AS owner_team_id, "
                + "COALESCE(NULLIF(u.full_name, ''), u.username) AS owner_name, "
                + "t.name AS owner_team_name, "
                + "r.created_at "
                + "FROM " + type.tableName() + " r "
                + "JOIN users u ON u.id = r.owner_user_id "
                + "LEFT JOIN teams t ON t.id = u.team_id "
                + "WHERE " + scopeClause + " "
                + "AND (? = '' OR r." + type.labelColumn() + " LIKE CONCAT('%', ?, '%')) "
                + "ORDER BY r.id DESC";

        String normalizedSearch = search == null ? "" : search.trim();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;

            if ("TEAM".equals(scope)) {
                if (actor.teamId() != null) {
                    stmt.setLong(index++, actor.teamId());
                    stmt.setLong(index++, actor.userId());
                } else {
                    stmt.setLong(index++, actor.userId());
                }
            } else if (!"ALL".equals(scope)) {
                stmt.setLong(index++, actor.userId());
            }

            stmt.setString(index++, normalizedSearch);
            stmt.setString(index, normalizedSearch);

            List<ScopeRecord> items = new ArrayList<>();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long teamId = rs.getLong("owner_team_id");
                    Long ownerTeamId = rs.wasNull() ? null : teamId;

                    Timestamp ts = rs.getTimestamp("created_at");
                    String createdAtStr = ts == null ? "" : ts.toLocalDateTime().format(DATE_FORMATTER);

                    items.add(new ScopeRecord(
                            rs.getLong("id"),
                            rs.getString("label"),
                            rs.getLong("owner_user_id"),
                            ownerTeamId,
                            rs.getString("owner_name"),
                            rs.getString("owner_team_name"),
                            createdAtStr
                    ));
                }
            }

            return items;
        }
    }

    public ScopeRecord findById(
            Connection conn,
            ScopeEntityType type,
            long id) throws SQLException {

        String sql = "SELECT r.id, r." + type.labelColumn() + " AS label, "
                + "r.owner_user_id, u.team_id AS owner_team_id, "
                + "COALESCE(NULLIF(u.full_name, ''), u.username) AS owner_name, "
                + "t.name AS owner_team_name, "
                + "r.created_at "
                + "FROM " + type.tableName() + " r "
                + "JOIN users u ON u.id = r.owner_user_id "
                + "LEFT JOIN teams t ON t.id = u.team_id "
                + "WHERE r.id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long teamId = rs.getLong("owner_team_id");
                Long ownerTeamId = rs.wasNull() ? null : teamId;

                Timestamp ts = rs.getTimestamp("created_at");
                String createdAtStr = ts == null ? "" : ts.toLocalDateTime().format(DATE_FORMATTER);

                return new ScopeRecord(
                        rs.getLong("id"),
                        rs.getString("label"),
                        rs.getLong("owner_user_id"),
                        ownerTeamId,
                        rs.getString("owner_name"),
                        rs.getString("owner_team_name"),
                        createdAtStr
                );
            }
        }
    }
}