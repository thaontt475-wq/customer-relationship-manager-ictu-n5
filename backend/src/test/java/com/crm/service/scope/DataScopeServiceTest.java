package com.crm.service.scope;

import com.crm.dao.scope.ScopedEntityDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-25 Data Scope Service:
 * Ensures User A (scope SELF / TEAM) can never read, update, or delete data of User B outside scope.
 */
@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

    private static final long USER_A_ID = 10L;
    private static final long TEAM_1_ID = 1L;

    private static final long USER_B_ID = 20L;
    private static final long TEAM_2_ID = 2L;

    @Mock
    private ScopedEntityDAO scopedEntityDAO;

    @Mock
    private UserDAO userDAO;

    private DataScopeService dataScopeService;

    @BeforeEach
    void setUp() {
        dataScopeService = new DataScopeService(scopedEntityDAO, userDAO, new ScopeAccessPolicy());
    }

    private User createMockUser(long id, Long teamId, String scope, String... roles) {
        User u = new User();
        u.setId(id);
        u.setTeamId(teamId);
        u.setDataScope(scope);
        if (roles != null && roles.length > 0) {
            u.setRoles(java.util.Arrays.stream(roles).map(r -> new Role(1L, r)).toList());
        }
        return u;
    }

    @Nested
    @DisplayName("SELF Scope Tests (Chỉ truy cập bản ghi của chính mình)")
    class SelfScopeTests {

        @Test
        @DisplayName("User A with SELF scope cannot read User B's record (returns FORBIDDEN 403)")
        void selfScope_cannotReadOtherUserRecord() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "SELF");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            ScopeRecord recordB = new ScopeRecord(100L, "Customer B", USER_B_ID, TEAM_1_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.CUSTOMERS), eq(100L))).thenReturn(recordB);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.CUSTOMERS, 100L);

            assertEquals(DataScopeService.ReadStatus.FORBIDDEN, result.status());
            assertNull(result.record());
        }

        @Test
        @DisplayName("User A with SELF scope can read own record (returns SUCCESS 200)")
        void selfScope_canReadOwnRecord() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "SELF");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            ScopeRecord recordA = new ScopeRecord(101L, "Customer A", USER_A_ID, TEAM_1_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.CUSTOMERS), eq(101L))).thenReturn(recordA);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.CUSTOMERS, 101L);

            assertEquals(DataScopeService.ReadStatus.SUCCESS, result.status());
            assertNotNull(result.record());
            assertEquals("Customer A", result.record().label());
        }

        @Test
        @DisplayName("User A with SELF scope cannot update User B's record (returns FORBIDDEN)")
        void selfScope_cannotUpdateOtherUserRecord() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "SELF");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            ScopeRecord recordB = new ScopeRecord(100L, "Opportunity B", USER_B_ID, TEAM_1_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.OPPORTUNITIES), eq(100L))).thenReturn(recordB);

            DataScopeService.OperationResult result = dataScopeService.update(USER_A_ID, ScopeEntityType.OPPORTUNITIES, 100L, "New Label");

            assertEquals(DataScopeService.ReadStatus.FORBIDDEN, result.status());
            verify(scopedEntityDAO, never()).update(any(Connection.class), any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("User A with SELF scope cannot delete User B's record (returns FORBIDDEN)")
        void selfScope_cannotDeleteOtherUserRecord() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "SELF");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            ScopeRecord recordB = new ScopeRecord(100L, "Activity B", USER_B_ID, TEAM_1_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.ACTIVITIES), eq(100L))).thenReturn(recordB);

            DataScopeService.ReadStatus status = dataScopeService.delete(USER_A_ID, ScopeEntityType.ACTIVITIES, 100L);

            assertEquals(DataScopeService.ReadStatus.FORBIDDEN, status);
            verify(scopedEntityDAO, never()).delete(any(Connection.class), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("TEAM Scope Tests (Truy cập cùng team, chặn khác team)")
    class TeamScopeTests {

        @Test
        @DisplayName("User with TEAM scope can read record of user in same team (SUCCESS)")
        void teamScope_canReadSameTeam() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "TEAM");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            // Record owned by another user (USER_B_ID) but in SAME team (TEAM_1_ID)
            ScopeRecord sameTeamRecord = new ScopeRecord(200L, "Quote Same Team", USER_B_ID, TEAM_1_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.QUOTES), eq(200L))).thenReturn(sameTeamRecord);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.QUOTES, 200L);

            assertEquals(DataScopeService.ReadStatus.SUCCESS, result.status());
            assertNotNull(result.record());
        }

        @Test
        @DisplayName("User with TEAM scope cannot read record of user in different team (FORBIDDEN 403)")
        void teamScope_cannotReadDifferentTeam() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "TEAM");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            // Record owned by user in DIFFERENT team (TEAM_2_ID)
            ScopeRecord otherTeamRecord = new ScopeRecord(201L, "Quote Other Team", USER_B_ID, TEAM_2_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.QUOTES), eq(201L))).thenReturn(otherTeamRecord);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.QUOTES, 201L);

            assertEquals(DataScopeService.ReadStatus.FORBIDDEN, result.status());
            assertNull(result.record());
        }

        @Test
        @DisplayName("User with TEAM scope without a team cannot access team records (FORBIDDEN)")
        void teamScope_userWithoutTeam_forbidden() throws SQLException {
            User userNoTeam = createMockUser(USER_A_ID, null, "TEAM");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userNoTeam);

            ScopeRecord record = new ScopeRecord(202L, "Customer", USER_B_ID, TEAM_1_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.CUSTOMERS), eq(202L))).thenReturn(record);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.CUSTOMERS, 202L);

            assertEquals(DataScopeService.ReadStatus.FORBIDDEN, result.status());
        }
    }

    @Nested
    @DisplayName("ALL Scope Tests (Toàn quyền hệ thống)")
    class AllScopeTests {

        @Test
        @DisplayName("User with ALL scope can read any record across any team and owner")
        void allScope_canReadAnyRecord() throws SQLException {
            User admin = createMockUser(USER_A_ID, null, "ALL", "admin");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(admin);

            ScopeRecord anyRecord = new ScopeRecord(300L, "Customer Any", 999L, 999L);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.CUSTOMERS), eq(300L))).thenReturn(anyRecord);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.CUSTOMERS, 300L);

            assertEquals(DataScopeService.ReadStatus.SUCCESS, result.status());
            assertNotNull(result.record());
        }

        @Test
        @DisplayName("Admin or Director role automatically gets ALL scope")
        void adminOrDirector_automaticallyResolvesToAllScope() throws SQLException {
            User director = createMockUser(USER_A_ID, TEAM_1_ID, null, "director");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(director);

            ScopeRecord otherRecord = new ScopeRecord(301L, "Customer", USER_B_ID, TEAM_2_ID);
            when(scopedEntityDAO.findById(any(Connection.class), eq(ScopeEntityType.CUSTOMERS), eq(301L))).thenReturn(otherRecord);

            DataScopeService.ReadResult result = dataScopeService.read(USER_A_ID, ScopeEntityType.CUSTOMERS, 301L);

            assertEquals(DataScopeService.ReadStatus.SUCCESS, result.status());
        }
    }

    @Nested
    @DisplayName("List and Export Tests")
    class ListAndExportTests {

        @Test
        @DisplayName("list delegates to DAO findVisible with user scope context")
        void list_delegatesToDao() throws SQLException {
            User userA = createMockUser(USER_A_ID, TEAM_1_ID, "SELF");
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_A_ID))).thenReturn(userA);

            ScopeRecord r1 = new ScopeRecord(1L, "Item 1", USER_A_ID, TEAM_1_ID);
            when(scopedEntityDAO.findVisible(any(Connection.class), eq(ScopeEntityType.CUSTOMERS), any(ScopeContext.class), eq("test")))
                    .thenReturn(List.of(r1));

            List<ScopeRecord> list = dataScopeService.list(USER_A_ID, ScopeEntityType.CUSTOMERS, "test");

            assertNotNull(list);
            assertEquals(1, list.size());
            assertEquals(USER_A_ID, list.get(0).ownerUserId());
        }
    }
}
