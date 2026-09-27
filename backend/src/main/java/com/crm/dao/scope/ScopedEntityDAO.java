package com.crm.dao.scope;

import com.crm.service.scope.ScopeContext;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScopedEntityDAO {

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
                    actor.teamId() == null ? "1=0" : "u.team_id = ?";
            default -> scopeClause = "r.owner_user_id = ?";
        }

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
            int index = 1;

            if ("TEAM".equals(scope) && actor.teamId() != null) {
                stmt.setLong(index++, actor.teamId());
            } else if (!"ALL".equals(scope) && !"TEAM".equals(scope)) {
                stmt.setLong(index++, actor.userId());
            }

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
}