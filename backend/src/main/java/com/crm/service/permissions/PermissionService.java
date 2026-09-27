package com.crm.service.permissions;

import com.crm.dao.permissions.PermissionDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PermissionService {
    private static final Set<String> DATA_SCOPES = Set.of("SELF", "TEAM", "ALL");

    private final PermissionDAO permissionDAO;
    private final UserDAO userDAO;

    public PermissionService() {
        this(new PermissionDAO(), new UserDAO());
    }

    PermissionService(PermissionDAO permissionDAO, UserDAO userDAO) {
        this.permissionDAO = permissionDAO;
        this.userDAO = userDAO;
    }

    public List<Role> findAllRoles() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return permissionDAO.findAllRoles(conn);
        }
    }

    public List<User> findAllUsers() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findAll(conn);
        }
    }

    public User findUserById(long userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findById(conn, userId);
        }
    }

    public List<Long> findRoleIdsByUserId(long userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return permissionDAO.findRoleIdsByUserId(conn, userId);
        }
    }

    public AssignmentResult assign(long actorUserId, long userId, List<Long> roleIds, String dataScope)
            throws SQLException {
        if (actorUserId <= 0 || userId <= 0) {
            return AssignmentResult.INVALID_USER;
        }

        String normalizedScope = dataScope == null
                ? ""
                : dataScope.trim().toUpperCase(Locale.ROOT);
        if (!DATA_SCOPES.contains(normalizedScope)) {
            return AssignmentResult.INVALID_DATA_SCOPE;
        }

        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        if (normalizedRoleIds == null) {
            return AssignmentResult.INVALID_ROLE;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User target = userDAO.findByIdForUpdate(conn, userId);
                if (target == null) {
                    conn.rollback();
                    return AssignmentResult.USER_NOT_FOUND;
                }

                if (!permissionDAO.allRolesExist(conn, normalizedRoleIds)) {
                    conn.rollback();
                    return AssignmentResult.INVALID_ROLE;
                }

                List<Role> roles = permissionDAO.findAllRoles(conn);
                Long teamLeadRoleId = findRoleIdByName(roles, "Team Lead");
                Long adminRoleId = findRoleIdByName(roles, "Admin");

                if (teamLeadRoleId != null
                        && normalizedRoleIds.contains(teamLeadRoleId)
                        && target.getTeamId() == null) {
                    conn.rollback();
                    return AssignmentResult.TEAM_REQUIRED;
                }

                if (actorUserId == userId && adminRoleId != null) {
                    List<Long> currentRoles = permissionDAO.findRoleIdsByUserId(conn, userId);
                    if (currentRoles.contains(adminRoleId)
                            && !normalizedRoleIds.contains(adminRoleId)) {
                        conn.rollback();
                        return AssignmentResult.CANNOT_REVOKE_OWN_ADMIN;
                    }
                }

                permissionDAO.replaceUserRoles(conn, userId, normalizedRoleIds);
                if (permissionDAO.updateDataScope(conn, userId, normalizedScope) != 1) {
                    conn.rollback();
                    return AssignmentResult.UPDATE_CONFLICT;
                }

                conn.commit();
                return AssignmentResult.SUCCESS;
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollbackException) { e.addSuppressed(rollbackException); }
                throw e;
            }
        }
    }

    private Long findRoleIdByName(List<Role> roles, String roleName) {
        for (Role role : roles) {
            if (role != null && role.getName() != null
                    && roleName.equalsIgnoreCase(role.getName().trim())) {
                return role.getId();
            }
        }
        return null;
    }

    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return List.of();
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            if (roleId == null || roleId <= 0) return null;
            unique.add(roleId);
        }
        return new ArrayList<>(unique);
    }

    public enum AssignmentResult {
        SUCCESS,
        INVALID_USER,
        INVALID_ROLE,
        INVALID_DATA_SCOPE,
        USER_NOT_FOUND,
        TEAM_REQUIRED,
        CANNOT_REVOKE_OWN_ADMIN,
        UPDATE_CONFLICT
    }
}