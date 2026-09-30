package com.crm.service.permissions;

import com.crm.dao.permissions.PermissionDAO;
import com.crm.dao.permissions.UserRoleDAO;
import com.crm.dao.teams.UserTeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.service.permissions.UserRoleService.RoleAssignmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-29 UserRoleService business logic.
 *
 * Covers:
 * - RULE 1: Multi-role support
 * - RULE 2: Team Lead validation
 * - RULE 3: Self-revoke Admin guard
 * - Team assignment & guards
 */
@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    private static final long ACTOR_ID = 1L;
    private static final long TARGET_ID = 2L;
    private static final long ADMIN_ROLE_ID = 10L;
    private static final long TEAM_LEAD_ROLE_ID = 20L;
    private static final long SALES_REP_ROLE_ID = 30L;

    @Mock private UserRoleDAO userRoleDAO;
    @Mock private UserTeamDAO userTeamDAO;
    @Mock private UserDAO userDAO;
    @Mock private PermissionDAO permissionDAO;
    @Mock private Connection conn;

    private UserRoleService service;

    // All roles available in the system
    private final List<Role> ALL_ROLES = List.of(
            new Role(ADMIN_ROLE_ID, "Admin"),
            new Role(TEAM_LEAD_ROLE_ID, "Team Lead"),
            new Role(SALES_REP_ROLE_ID, "Sales Rep")
    );

    @BeforeEach
    void setUp() {
        service = new UserRoleService(userRoleDAO, userTeamDAO, userDAO, permissionDAO);
    }

    // ===== Invalid input guard tests =====

    @Test
    @DisplayName("INVALID_USER: actorUserId <= 0 returns INVALID_USER")
    void invalidActorId_returnsInvalidUser() throws SQLException {
        RoleAssignmentResult result = service.assignRoles(0L, TARGET_ID, List.of(SALES_REP_ROLE_ID));
        assertEquals(RoleAssignmentResult.INVALID_USER, result);
    }

    @Test
    @DisplayName("INVALID_USER: targetUserId <= 0 returns INVALID_USER")
    void invalidTargetId_returnsInvalidUser() throws SQLException {
        RoleAssignmentResult result = service.assignRoles(ACTOR_ID, -1L, List.of(SALES_REP_ROLE_ID));
        assertEquals(RoleAssignmentResult.INVALID_USER, result);
    }

    @Test
    @DisplayName("INVALID_ROLE: null roleId in list returns INVALID_ROLE")
    void nullRoleIdInList_returnsInvalidRole() throws SQLException {
        RoleAssignmentResult result = service.assignRoles(ACTOR_ID, TARGET_ID, List.of(1L, null, 3L));
        assertEquals(RoleAssignmentResult.INVALID_ROLE, result);
    }

    @Test
    @DisplayName("INVALID_ROLE: roleId <= 0 in list returns INVALID_ROLE")
    void negativeRoleIdInList_returnsInvalidRole() throws SQLException {
        RoleAssignmentResult result = service.assignRoles(ACTOR_ID, TARGET_ID, List.of(-5L));
        assertEquals(RoleAssignmentResult.INVALID_ROLE, result);
    }

    // ===== USER_NOT_FOUND =====

    @Nested
    @DisplayName("USER_NOT_FOUND scenarios")
    class UserNotFoundTests {

        @Test
        @DisplayName("Target user not in DB returns USER_NOT_FOUND")
        void targetUserNotFound_returnsUserNotFound() throws SQLException {
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(null);

            RoleAssignmentResult result = service.assignRoles(ACTOR_ID, TARGET_ID, List.of(SALES_REP_ROLE_ID));
            assertEquals(RoleAssignmentResult.USER_NOT_FOUND, result);
        }
    }

    // ===== RULE 1: Multi-role =====

    @Nested
    @DisplayName("RULE 1: Multi-role support")
    class MultiRoleTests {

        @Test
        @DisplayName("Assigning multiple valid roles succeeds")
        void assignMultipleRoles_succeeds() throws SQLException {
            User target = mockUser(TARGET_ID, 99L);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userTeamDAO.findTeamIdByUserId(any(Connection.class), eq(TARGET_ID))).thenReturn(99L);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            // Assign Admin + Team Lead + Sales Rep simultaneously (RULE 1: multi-role)
            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(ADMIN_ROLE_ID, TEAM_LEAD_ROLE_ID, SALES_REP_ROLE_ID));

            assertEquals(RoleAssignmentResult.SUCCESS, result);
            verify(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID),
                    argThat(ids -> ids.containsAll(List.of(ADMIN_ROLE_ID, TEAM_LEAD_ROLE_ID, SALES_REP_ROLE_ID))));
        }

        @Test
        @DisplayName("Duplicate roleIds are deduplicated before assignment")
        void duplicateRoleIds_areDeduplicated() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            // Sales Rep repeated twice — should deduplicate to 1
            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(SALES_REP_ROLE_ID, SALES_REP_ROLE_ID));

            assertEquals(RoleAssignmentResult.SUCCESS, result);
            verify(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID),
                    argThat(ids -> ids.size() == 1 && ids.contains(SALES_REP_ROLE_ID)));
        }

        @Test
        @DisplayName("Empty roleIds list clears all roles (allowed)")
        void emptyRoleIdsList_clearsAllRoles() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            RoleAssignmentResult result = service.assignRoles(ACTOR_ID, TARGET_ID, List.of());
            assertEquals(RoleAssignmentResult.SUCCESS, result);
            verify(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), eq(List.of()));
        }
    }

    // ===== RULE 2: Team Lead validation =====

    @Nested
    @DisplayName("RULE 2: Team Lead requires team membership")
    class TeamLeadValidationTests {

        @Test
        @DisplayName("Assigning Team Lead to user WITHOUT a team returns TEAM_REQUIRED")
        void teamLeadWithoutTeam_returnsTeamRequired() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userTeamDAO.findTeamIdByUserId(any(Connection.class), eq(TARGET_ID))).thenReturn(null);

            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(TEAM_LEAD_ROLE_ID));

            assertEquals(RoleAssignmentResult.TEAM_REQUIRED, result);
            verify(userRoleDAO, never()).replaceUserRoles(any(), anyLong(), anyList());
        }

        @Test
        @DisplayName("Assigning Team Lead to user WITH a team succeeds")
        void teamLeadWithTeam_succeeds() throws SQLException {
            User target = mockUser(TARGET_ID, 5L);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userTeamDAO.findTeamIdByUserId(any(Connection.class), eq(TARGET_ID))).thenReturn(5L);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(TEAM_LEAD_ROLE_ID));

            assertEquals(RoleAssignmentResult.SUCCESS, result);
        }

        @Test
        @DisplayName("Assigning Team Lead and team simultaneously succeeds")
        void teamLeadWithSimultaneousTeamAssignment_succeeds() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(userTeamDAO.teamExists(any(Connection.class), eq(100L))).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userTeamDAO.findTeamIdByUserId(any(Connection.class), eq(TARGET_ID))).thenReturn(100L);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(TEAM_LEAD_ROLE_ID), 100L);

            assertEquals(RoleAssignmentResult.SUCCESS, result);
            verify(userTeamDAO).assignUserToTeam(any(Connection.class), eq(TARGET_ID), eq(100L));
        }

        @Test
        @DisplayName("Non-Team-Lead roles don't require a team")
        void nonTeamLeadRoles_doNotRequireTeam() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(SALES_REP_ROLE_ID));

            assertEquals(RoleAssignmentResult.SUCCESS, result);
            verify(userTeamDAO, never()).findTeamIdByUserId(any(Connection.class), anyLong());
        }

        @Test
        @DisplayName("assignTeam: Removing team from user holding Team Lead role returns TEAM_REQUIRED")
        void removeTeamFromTeamLead_returnsTeamRequired() throws SQLException {
            User target = mockUser(TARGET_ID, 5L);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userRoleDAO.findRoleIdsByUserId(any(Connection.class), eq(TARGET_ID)))
                    .thenReturn(List.of(TEAM_LEAD_ROLE_ID));

            RoleAssignmentResult result = service.assignTeam(ACTOR_ID, TARGET_ID, null);

            assertEquals(RoleAssignmentResult.TEAM_REQUIRED, result);
            verify(userTeamDAO, never()).removeUserFromTeam(any(Connection.class), anyLong());
        }
    }

    // ===== RULE 3: Self-revoke Admin guard =====

    @Nested
    @DisplayName("RULE 3: Admin cannot revoke own Admin role")
    class SelfRevokeAdminTests {

        @Test
        @DisplayName("Admin removing own Admin role returns CANNOT_REVOKE_OWN_ADMIN")
        void adminRevokingOwnAdmin_returnsCannotRevokeOwnAdmin() throws SQLException {
            long actorAndTarget = 1L;
            User target = mockUser(actorAndTarget, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(actorAndTarget))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userRoleDAO.findRoleIdsByUserId(any(Connection.class), eq(actorAndTarget)))
                    .thenReturn(List.of(ADMIN_ROLE_ID));

            // Trying to assign only Sales Rep (removes Admin)
            RoleAssignmentResult result = service.assignRoles(
                    actorAndTarget, actorAndTarget, List.of(SALES_REP_ROLE_ID));

            assertEquals(RoleAssignmentResult.CANNOT_REVOKE_OWN_ADMIN, result);
            verify(userRoleDAO, never()).replaceUserRoles(any(), anyLong(), anyList());
        }

        @Test
        @DisplayName("Admin keeping their own Admin role succeeds")
        void adminKeepingOwnAdmin_succeeds() throws SQLException {
            long actorAndTarget = 1L;
            User target = mockUser(actorAndTarget, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(actorAndTarget))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userRoleDAO.findRoleIdsByUserId(any(Connection.class), eq(actorAndTarget)))
                    .thenReturn(List.of(ADMIN_ROLE_ID));
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(actorAndTarget), anyList());

            // Keeping Admin + adding Sales Rep
            RoleAssignmentResult result = service.assignRoles(
                    actorAndTarget, actorAndTarget, List.of(ADMIN_ROLE_ID, SALES_REP_ROLE_ID));

            assertEquals(RoleAssignmentResult.SUCCESS, result);
        }

        @Test
        @DisplayName("Admin removing another user's Admin role is allowed")
        void adminRemovingOtherUserAdmin_succeeds() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.allRolesExist(any(Connection.class), anyList())).thenReturn(true);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            // ACTOR_ID != TARGET_ID → self-revoke check skipped
            RoleAssignmentResult result = service.assignRoles(
                    ACTOR_ID, TARGET_ID, List.of(SALES_REP_ROLE_ID));

            assertEquals(RoleAssignmentResult.SUCCESS, result);
            verify(userRoleDAO, never()).findRoleIdsByUserId(any(Connection.class), eq(TARGET_ID));
        }

        @Test
        @DisplayName("removeRole: Admin removing own Admin role returns CANNOT_REVOKE_OWN_ADMIN")
        void removeRole_adminRemovingOwnAdmin_blocked() throws SQLException {
            long actorAndTarget = 1L;
            User target = mockUser(actorAndTarget, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(actorAndTarget))).thenReturn(target);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);

            RoleAssignmentResult result = service.removeRole(actorAndTarget, actorAndTarget, ADMIN_ROLE_ID);

            assertEquals(RoleAssignmentResult.CANNOT_REVOKE_OWN_ADMIN, result);
        }

        @Test
        @DisplayName("removeRole: Admin removing own non-Admin role is allowed")
        void removeRole_adminRemovingOwnSalesRep_allowed() throws SQLException {
            long actorAndTarget = 1L;
            User target = mockUser(actorAndTarget, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(actorAndTarget))).thenReturn(target);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userRoleDAO.findRoleIdsByUserId(any(Connection.class), eq(actorAndTarget)))
                    .thenReturn(List.of(ADMIN_ROLE_ID, SALES_REP_ROLE_ID));
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(actorAndTarget), anyList());

            RoleAssignmentResult result = service.removeRole(actorAndTarget, actorAndTarget, SALES_REP_ROLE_ID);

            assertEquals(RoleAssignmentResult.SUCCESS, result);
        }
    }

    // ===== addRole tests =====

    @Nested
    @DisplayName("addRole: Add single role to user")
    class AddRoleTests {

        @Test
        @DisplayName("Adding Team Lead to user with team succeeds")
        void addTeamLeadWithTeam_succeeds() throws SQLException {
            User target = mockUser(TARGET_ID, 7L);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userTeamDAO.findTeamIdByUserId(any(Connection.class), eq(TARGET_ID))).thenReturn(7L);
            when(userRoleDAO.findRoleIdsByUserId(any(Connection.class), eq(TARGET_ID)))
                    .thenReturn(List.of(SALES_REP_ROLE_ID));
            doNothing().when(userRoleDAO).replaceUserRoles(any(Connection.class), eq(TARGET_ID), anyList());

            RoleAssignmentResult result = service.addRole(ACTOR_ID, TARGET_ID, TEAM_LEAD_ROLE_ID);
            assertEquals(RoleAssignmentResult.SUCCESS, result);
        }

        @Test
        @DisplayName("Adding Team Lead to user without team returns TEAM_REQUIRED")
        void addTeamLeadWithoutTeam_returnsTeamRequired() throws SQLException {
            User target = mockUser(TARGET_ID, null);
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(TARGET_ID))).thenReturn(target);
            when(permissionDAO.findAllRoles(any(Connection.class))).thenReturn(ALL_ROLES);
            when(userTeamDAO.findTeamIdByUserId(any(Connection.class), eq(TARGET_ID))).thenReturn(null);

            RoleAssignmentResult result = service.addRole(ACTOR_ID, TARGET_ID, TEAM_LEAD_ROLE_ID);
            assertEquals(RoleAssignmentResult.TEAM_REQUIRED, result);
        }
    }

    // ===== Utility =====

    private User mockUser(long id, Long teamId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getTeamId()).thenReturn(teamId);
        return user;
    }
}
