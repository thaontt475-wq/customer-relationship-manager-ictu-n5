package com.crm.dao.scope;

import com.crm.service.scope.ScopeContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;

/**
 * DAO Helper for constructing SQL WHERE clauses based on Data Scope (SELF / TEAM / ALL).
 * Satisfies CRM-25 Architecture & Technical Guidelines.
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
    }

    /**
     * Builds the SQL condition based on the user's data scope:
     * - ALL: "1=1"
     * - TEAM: userTableAlias.team_id = ? (or 1=0 if user has no team)
     * - SELF: ownerColumnAlias = ?
     *
     * @param ownerColumn      e.g. "r.owner_user_id"
     * @param userTableAlias   e.g. "u" (joined table with team_id)
     * @param actor            ScopeContext of current user
     * @return SQL condition snippet
     */
    public static String buildScopeClause(String ownerColumn, String userTableAlias, ScopeContext actor) {
        if (actor == null) {
            return "1=0";
        }

        String scope = actor.dataScope() == null
                ? "SELF"
                : actor.dataScope().trim().toUpperCase(Locale.ROOT);

        return switch (scope) {
            case "ALL" -> "1=1";
            case "TEAM" -> actor.teamId() == null
                    ? "1=0"
                    : (userTableAlias != null ? userTableAlias + ".team_id = ?" : ownerColumn + " IN (SELECT id FROM users WHERE team_id = ?)");
            default -> ownerColumn + " = ?";
        };
    }

    /**
     * Binds the necessary parameter(s) for the scope clause to a PreparedStatement.
     *
     * @param stmt       PreparedStatement
     * @param startIndex 1-based parameter index to start at
     * @param actor      ScopeContext of current user
     * @return Next available parameter index
     */
    public static int bindScopeParameters(PreparedStatement stmt, int startIndex, ScopeContext actor) throws SQLException {
        if (actor == null) {
            return startIndex;
        }

        String scope = actor.dataScope() == null
                ? "SELF"
                : actor.dataScope().trim().toUpperCase(Locale.ROOT);

        int index = startIndex;
        if ("TEAM".equals(scope) && actor.teamId() != null) {
            stmt.setLong(index++, actor.teamId());
        } else if (!"ALL".equals(scope) && !"TEAM".equals(scope)) {
            stmt.setLong(index++, actor.userId());
        }
        return index;
    }

    /**
     * Resolves the effective data scope string for a user given their explicit data_scope and roles.
     * Directors and Admins automatically receive "ALL" scope.
     * Team leads default to "TEAM" if data_scope is not set.
     * Others default to "SELF".
     */
    public static String resolveEffectiveScope(String explicitScope, Collection<String> roles) {
        if (roles != null) {
            boolean isAdminOrDirector = roles.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .map(r -> r.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(r -> "admin".equals(r) || "director".equals(r) || "giám đốc".equals(r) || "quản trị viên".equals(r));
            if (isAdminOrDirector) {
                return "ALL";
            }
        }

        if (explicitScope != null && !explicitScope.isBlank()) {
            return explicitScope.trim().toUpperCase(Locale.ROOT);
        }

        if (roles != null) {
            boolean isTeamLead = roles.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .map(r -> r.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(r -> "team lead".equals(r) || "trưởng nhóm".equals(r));
            if (isTeamLead) {
                return "TEAM";
            }
        }

        return "SELF";
    }
}
