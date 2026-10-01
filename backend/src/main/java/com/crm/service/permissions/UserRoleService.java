package com.crm.service.permissions;

import com.crm.dao.permissions.PermissionDAO;
import com.crm.dao.permissions.UserRoleDAO;
import com.crm.dao.teams.UserTeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.service.audit.AuditLogService;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service handling role and team assignment with business logic validation (CRM-29).
 * Architecture: Servlet -> UserRoleService -> UserRoleDAO / UserTeamDAO -> JDBC -> MySQL 8.0.
 *
 * Business Logic Constraints:
 * 1. Multi-role: A user can hold multiple roles simultaneously.
 * 2. Team Lead validation: "Team Lead" role requires user to belong to a team (team_id not null).
 * 3. Self-revoke Admin guard: An admin cannot revoke the Admin role from themselves.
 */
public class UserRoleService {
    private static final Logger LOGGER = Logger.getLogger(UserRoleService.class.getName());

    public static final String ROLE_TEAM_LEAD = "Team Lead";
    public static final String ROLE_ADMIN = "Admin";

    private final UserRoleDAO userRoleDAO;
    private final UserTeamDAO userTeamDAO;
    private final UserDAO userDAO;
    private final PermissionDAO permissionDAO;
    private final AuditLogService auditLogService;

    public UserRoleService() {
        this(new UserRoleDAO(), new UserTeamDAO(), new UserDAO(), new PermissionDAO(),
                new AuditLogService());
    }

    public UserRoleService(UserRoleDAO userRoleDAO, UserTeamDAO userTeamDAO, UserDAO userDAO, PermissionDAO permissionDAO) {
        this(userRoleDAO, userTeamDAO, userDAO, permissionDAO, new AuditLogService());
    }

    public UserRoleService(UserRoleDAO userRoleDAO, UserTeamDAO userTeamDAO, UserDAO userDAO,
                           PermissionDAO permissionDAO, AuditLogService auditLogService) {
        this.userRoleDAO = userRoleDAO != null ? userRoleDAO : new UserRoleDAO();
        this.userTeamDAO = userTeamDAO != null ? userTeamDAO : new UserTeamDAO();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
        this.permissionDAO = permissionDAO != null ? permissionDAO : new PermissionDAO();
        this.auditLogService = auditLogService != null ? auditLogService : new AuditLogService();
    }

    /**
     * Retrieve all available roles from the database.
     */
    public List<Role> findAllRoles() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return permissionDAO.findAllRoles(conn);
        }
    }

    /**
     * Retrieve all roles currently assigned to a user.
     */
    public List<Role> findRolesByUserId(long userId) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        try (Connection conn = DBConnection.getConnection()) {
            return userRoleDAO.findRolesByUserId(conn, userId);
        }
    }

    /**
     * Retrieve role IDs currently assigned to a user.
     */
    public List<Long> findRoleIdsByUserId(long userId) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        try (Connection conn = DBConnection.getConnection()) {
            return userRoleDAO.findRoleIdsByUserId(conn, userId);
        }
    }

    /**
     * Retrieve team ID assigned to a user.
     */
    public Long findTeamIdByUserId(long userId) throws SQLException {
        if (userId <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return userTeamDAO.findTeamIdByUserId(conn, userId);
        }
    }

    /**
     * Assign multiple roles to a user (CRM-29 Business Logic).
     *
     * Validates:
     * 1. Multi-role: Multiple roleIds accepted simultaneously.
     * 2. Team Lead requires team: If "Team Lead" is in roleIds, user must have a team assigned.
     * 3. Self-revoke Admin guard: actorUserId cannot remove Admin role from themselves.
     *
     * @param actorUserId  user performing the action (for self-revoke check)
     * @param targetUserId user being modified
     * @param roleIds      list of new role IDs to assign (replaces all current roles)
     * @return RoleAssignmentResult indicating success or specific failure reason
     */
    public RoleAssignmentResult assignRoles(long actorUserId, long targetUserId, List<Long> roleIds)
            throws SQLException {
        return assignRoles(actorUserId, targetUserId, roleIds, null);
    }

    /**
     * Assign multiple roles and optionally assign/update team for a user atomically.
     *
     * @param actorUserId  user performing the action
     * @param targetUserId user being modified
     * @param roleIds      new role IDs (replaces all current roles)
     * @param newTeamId    optional new team ID to assign (null means keep current team unless empty)
     * @return RoleAssignmentResult indicating success or specific failure reason
     */
    public RoleAssignmentResult assignRoles(long actorUserId, long targetUserId, List<Long> roleIds, Long newTeamId)
            throws SQLException {

        if (actorUserId <= 0 || targetUserId <= 0) {
            return RoleAssignmentResult.INVALID_USER;
        }

        // Normalize and deduplicate roleIds (multi-role support: RULE 1)
        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        if (normalizedRoleIds == null) {
            return RoleAssignmentResult.INVALID_ROLE;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Verify target user exists
                User target = userDAO.findByIdForUpdate(conn, targetUserId);
                if (target == null) {
                    conn.rollback();
                    return RoleAssignmentResult.USER_NOT_FOUND;
                }
                List<Long> currentRoleIds = userRoleDAO.findRoleIdsByUserId(conn, targetUserId);

                // Verify all role IDs exist in DB
                if (!normalizedRoleIds.isEmpty() && !permissionDAO.allRolesExist(conn, normalizedRoleIds)) {
                    conn.rollback();
                    return RoleAssignmentResult.INVALID_ROLE;
                }

                // If newTeamId is specified (> 0), verify and update user's team
                if (newTeamId != null) {
                    if (newTeamId > 0) {
                        if (!userTeamDAO.teamExists(conn, newTeamId)) {
                            conn.rollback();
                            return RoleAssignmentResult.TEAM_NOT_FOUND;
                        }
                        userTeamDAO.assignUserToTeam(conn, targetUserId, newTeamId);
                    } else {
                        userTeamDAO.removeUserFromTeam(conn, targetUserId);
                    }
                }

                // Load all roles for name-based lookup
                List<Role> allRoles = permissionDAO.findAllRoles(conn);

                // RULE 2: Team Lead validation — must belong to a team
                boolean isAssigningTeamLead = isAnyRoleMatching(allRoles, normalizedRoleIds, this::isTeamLeadRoleName);
                if (isAssigningTeamLead) {
                    Long currentTeamId = userTeamDAO.findTeamIdByUserId(conn, targetUserId);
                    if (currentTeamId == null || currentTeamId <= 0) {
                        conn.rollback();
                        return RoleAssignmentResult.TEAM_REQUIRED;
                    }
                }

                // RULE 3: Self-revoke Admin guard — admin cannot remove own admin role
                if (actorUserId == targetUserId) {
                    boolean currentlyAdmin = isAnyRoleMatching(allRoles, currentRoleIds, this::isAdminRoleName);
                    boolean willHaveAdmin = isAnyRoleMatching(allRoles, normalizedRoleIds, this::isAdminRoleName);

                    if (currentlyAdmin && !willHaveAdmin) {
                        conn.rollback();
                        return RoleAssignmentResult.CANNOT_REVOKE_OWN_ADMIN;
                    }
                }

                // RULE 1: Multi-role — replace all roles with normalized list
                userRoleDAO.replaceUserRoles(conn, targetUserId, normalizedRoleIds);
                recordRoleChangeIfNeeded(conn, actorUserId, targetUserId,
                        currentRoleIds, normalizedRoleIds);

                conn.commit();
                return RoleAssignmentResult.SUCCESS;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Assign or reassign a user to a team (or remove if teamId <= 0 or null).
     * Enforces RULE 2: A user holding Team Lead role cannot have their team removed.
     */
    public RoleAssignmentResult assignTeam(long actorUserId, long targetUserId, Long teamId) throws SQLException {
        if (actorUserId <= 0 || targetUserId <= 0) {
            return RoleAssignmentResult.INVALID_USER;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                User target = userDAO.findByIdForUpdate(conn, targetUserId);
                if (target == null) {
                    conn.rollback();
                    return RoleAssignmentResult.USER_NOT_FOUND;
                }

                // If removing team (teamId is null or <= 0)
                if (teamId == null || teamId <= 0) {
                    List<Role> allRoles = permissionDAO.findAllRoles(conn);
                    List<Long> currentRoleIds = userRoleDAO.findRoleIdsByUserId(conn, targetUserId);
                    boolean isTeamLead = isAnyRoleMatching(allRoles, currentRoleIds, this::isTeamLeadRoleName);

                    if (isTeamLead) {
                        conn.rollback();
                        return RoleAssignmentResult.TEAM_REQUIRED;
                    }
                    userTeamDAO.removeUserFromTeam(conn, targetUserId);
                } else {
                    // Assigning to a specific team
                    if (!userTeamDAO.teamExists(conn, teamId)) {
                        conn.rollback();
                        return RoleAssignmentResult.TEAM_NOT_FOUND;
                    }
                    userTeamDAO.assignUserToTeam(conn, targetUserId, teamId);
                }

                conn.commit();
                return RoleAssignmentResult.SUCCESS;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Add a single role to a user (without removing existing roles).
     * Enforces Team Lead validation.
     */
    public RoleAssignmentResult addRole(long actorUserId, long targetUserId, long roleId) throws SQLException {
        if (actorUserId <= 0 || targetUserId <= 0 || roleId <= 0) {
            return RoleAssignmentResult.INVALID_USER;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                User target = userDAO.findByIdForUpdate(conn, targetUserId);
                if (target == null) {
                    conn.rollback();
                    return RoleAssignmentResult.USER_NOT_FOUND;
                }

                List<Role> allRoles = permissionDAO.findAllRoles(conn);
                Role roleToAdd = findRoleById(allRoles, roleId);
                if (roleToAdd == null) {
                    conn.rollback();
                    return RoleAssignmentResult.INVALID_ROLE;
                }

                // RULE 2: If adding Team Lead, user must belong to a team
                if (isTeamLeadRoleName(roleToAdd.getName())) {
                    Long teamId = userTeamDAO.findTeamIdByUserId(conn, targetUserId);
                    if (teamId == null || teamId <= 0) {
                        conn.rollback();
                        return RoleAssignmentResult.TEAM_REQUIRED;
                    }
                }

                // Merge with current roles
                List<Long> currentRoleIds = new ArrayList<>(userRoleDAO.findRoleIdsByUserId(conn, targetUserId));
                List<Long> previousRoleIds = new ArrayList<>(currentRoleIds);
                if (!currentRoleIds.contains(roleId)) {
                    currentRoleIds.add(roleId);
                }
                userRoleDAO.replaceUserRoles(conn, targetUserId, currentRoleIds);
                recordRoleChangeIfNeeded(conn, actorUserId, targetUserId,
                        previousRoleIds, currentRoleIds);

                conn.commit();
                return RoleAssignmentResult.SUCCESS;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Remove a single role from a user.
     * Enforces RULE 3: Self-revoke Admin guard.
     */
    public RoleAssignmentResult removeRole(long actorUserId, long targetUserId, long roleId) throws SQLException {
        if (actorUserId <= 0 || targetUserId <= 0 || roleId <= 0) {
            return RoleAssignmentResult.INVALID_USER;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                User target = userDAO.findByIdForUpdate(conn, targetUserId);
                if (target == null) {
                    conn.rollback();
                    return RoleAssignmentResult.USER_NOT_FOUND;
                }

                List<Role> allRoles = permissionDAO.findAllRoles(conn);
                Role roleToRemove = findRoleById(allRoles, roleId);

                // RULE 3: Self-revoke Admin guard
                if (actorUserId == targetUserId && roleToRemove != null && isAdminRoleName(roleToRemove.getName())) {
                    conn.rollback();
                    return RoleAssignmentResult.CANNOT_REVOKE_OWN_ADMIN;
                }

                List<Long> currentRoleIds = new ArrayList<>(userRoleDAO.findRoleIdsByUserId(conn, targetUserId));
                List<Long> previousRoleIds = new ArrayList<>(currentRoleIds);
                currentRoleIds.removeIf(id -> id != null && id == roleId);
                userRoleDAO.replaceUserRoles(conn, targetUserId, currentRoleIds);
                recordRoleChangeIfNeeded(conn, actorUserId, targetUserId,
                        previousRoleIds, currentRoleIds);

                conn.commit();
                return RoleAssignmentResult.SUCCESS;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    // === Helpers ===

    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            if (roleId == null || roleId <= 0) {
                return null;
            }
            unique.add(roleId);
        }
        return new ArrayList<>(unique);
    }

    private void recordRoleChangeIfNeeded(Connection conn, long actorUserId, long targetUserId,
                                          Collection<Long> beforeRoleIds,
                                          Collection<Long> afterRoleIds) throws SQLException {
        LinkedHashSet<Long> before = new LinkedHashSet<>(beforeRoleIds == null ? List.of() : beforeRoleIds);
        LinkedHashSet<Long> after = new LinkedHashSet<>(afterRoleIds == null ? List.of() : afterRoleIds);
        if (before.equals(after)) {
            return;
        }
        auditLogService.recordRoleChange(conn, actorUserId, targetUserId,
                Map.of("roleIds", new ArrayList<>(before)),
                Map.of("roleIds", new ArrayList<>(after)));
    }

    private Role findRoleById(List<Role> roles, long roleId) {
        if (roles == null) {
            return null;
        }
        for (Role role : roles) {
            if (role != null && role.getId() == roleId) {
                return role;
            }
        }
        return null;
    }

    private boolean isAnyRoleMatching(List<Role> allRoles, Collection<Long> roleIds,
                                      java.util.function.Predicate<String> matcher) {
        if (allRoles == null || roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        for (Role role : allRoles) {
            if (role != null && roleIds.contains(role.getId()) && matcher.test(role.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isTeamLeadRoleName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
        return lower.equals("team lead") || lower.contains("lead") || lower.contains("trưởng nhóm");
    }

    public boolean isAdminRoleName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
        return lower.equals("admin") || lower.contains("admin") || lower.contains("quản trị");
    }

    /**
     * Result codes for role and team assignment operations.
     */
    public enum RoleAssignmentResult {
        /** Operation completed successfully. */
        SUCCESS,
        /** Actor or target user ID is invalid (<= 0). */
        INVALID_USER,
        /** One or more role IDs are null, non-positive, or do not exist in the DB. */
        INVALID_ROLE,
        /** Target user was not found in the database. */
        USER_NOT_FOUND,
        /**
         * "Team Lead" role was requested but the target user is not assigned to any team.
         * (RULE 2 violation)
         */
        TEAM_REQUIRED,
        /**
         * The actor is attempting to remove their own Admin role.
         * (RULE 3 violation)
         */
        CANNOT_REVOKE_OWN_ADMIN,
        /**
         * Specified team ID does not exist in the database.
         */
        TEAM_NOT_FOUND
    }
}
