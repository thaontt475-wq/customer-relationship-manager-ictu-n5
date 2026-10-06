package com.crm.service.permissions;

import java.util.List;
import java.util.Set;

public record DataScopeContext(
        long currentUserId,
        String module,
        ScopeType scopeType,
        Long teamId,
        Set<Long> allowedOwnerIds
) {
    public DataScopeContext {
        allowedOwnerIds = Set.copyOf(allowedOwnerIds);
    }

    public boolean canAccessOwner(long ownerId) {
        return scopeType == ScopeType.ALL || allowedOwnerIds.contains(ownerId);
    }

    /** Shared bound predicate for scoped list, detail, search, filter and export DAOs. */
    public void appendOwnerPredicate(String column, StringBuilder sql, List<Object> parameters) {
        if (!column.matches("[A-Za-z_][A-Za-z0-9_.]*")) {
            throw new IllegalArgumentException("Tên cột sở hữu không hợp lệ");
        }
        if (scopeType == ScopeType.ALL) return;
        if (allowedOwnerIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND ").append(column).append(" IN (");
        int index = 0;
        for (Long id : allowedOwnerIds) {
            if (index++ > 0) sql.append(',');
            sql.append('?');
            parameters.add(id);
        }
        sql.append(')');
    }
}
