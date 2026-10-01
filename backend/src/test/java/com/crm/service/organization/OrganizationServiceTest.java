package com.crm.service.organization;

import com.crm.dao.organization.OrganizationDAO;
import com.crm.dao.teams.UserTeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Organization;
import com.crm.model.User;
import com.crm.service.organization.OrganizationService.ErrorCode;
import com.crm.service.organization.OrganizationService.OrganizationException;
import com.crm.service.organization.OrganizationService.UnitInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationDAO organizationDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private UserTeamDAO userTeamDAO;
    @Mock
    private OrganizationService.ConnectionProvider connectionProvider;
    @Mock
    private Connection connection;

    private OrganizationService service;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(connectionProvider.getConnection()).thenReturn(connection);
        lenient().when(connection.getAutoCommit()).thenReturn(true);
        service = new OrganizationService(
                organizationDAO,
                userDAO,
                userTeamDAO,
                connectionProvider
        );
    }

    @Test
    @DisplayName("Create unit persists metadata and assigns its active leader as the first member")
    void createTeam() throws Exception {
        User leader = activeUser(7L, null);
        Organization created = unit(50L, null, 7L, "NORTH");

        when(organizationDAO.existsByNameExcluding(connection, "Miền Bắc", null)).thenReturn(false);
        when(userDAO.findByIdForUpdate(connection, 7L)).thenReturn(leader);
        when(organizationDAO.findUnitIdByLeader(connection, 7L)).thenReturn(null);
        when(organizationDAO.insert(connection, "Miền Bắc", null, 7L, "NORTH", true))
                .thenReturn(50L);
        when(userTeamDAO.assignUserToTeam(connection, 7L, 50L)).thenReturn(1);
        when(organizationDAO.findById(connection, 50L)).thenReturn(created);

        Organization result = service.createUnit(
                new UnitInput("  Miền Bắc  ", null, 7L, "north", true)
        );

        assertSame(created, result);
        verify(organizationDAO).insert(connection, "Miền Bắc", null, 7L, "NORTH", true);
        verify(userTeamDAO).assignUserToTeam(connection, 7L, 50L);
        verify(connection).commit();
    }

    @Test
    @DisplayName("Update can move a unit below an existing parent atomically")
    void updateParent() throws Exception {
        Organization current = unit(10L, null, 1L, "NATIONAL");
        Organization parent = unit(20L, null, 2L, "NORTH");
        Organization updated = unit(10L, 20L, 1L, "NATIONAL");
        User leader = activeUser(1L, 10L);

        when(organizationDAO.findByIdForUpdate(connection, 10L)).thenReturn(current);
        when(organizationDAO.findByIdForUpdate(connection, 20L)).thenReturn(parent);
        when(organizationDAO.existsByNameExcluding(connection, "Kinh doanh", 10L)).thenReturn(false);
        when(userDAO.findByIdForUpdate(connection, 1L)).thenReturn(leader);
        when(organizationDAO.findUnitIdByLeader(connection, 1L)).thenReturn(10L);
        when(organizationDAO.update(connection, 10L, "Kinh doanh", 20L, 1L,
                "NATIONAL", true)).thenReturn(1);
        when(organizationDAO.findById(connection, 10L)).thenReturn(updated);

        Organization result = service.updateUnit(10L,
                new UnitInput("Kinh doanh", 20L, 1L, "NATIONAL", true));

        assertEquals(20L, result.getParentId());
        verify(connection).commit();
    }

    @Test
    @DisplayName("A unit cannot be its own parent")
    void preventSelfParent() throws Exception {
        when(organizationDAO.findByIdForUpdate(connection, 10L))
                .thenReturn(unit(10L, null, 1L, "NATIONAL"));

        OrganizationException error = assertThrows(OrganizationException.class,
                () -> service.updateUnit(10L,
                        new UnitInput("Kinh doanh", 10L, 1L, "NATIONAL", true)));

        assertEquals(ErrorCode.SELF_PARENT, error.getCode());
        verify(organizationDAO, never()).update(eq(connection), anyLong(), anyString(),
                anyLong(), anyLong(), anyString(), anyBoolean());
        verify(connection).rollback();
    }

    @Test
    @DisplayName("A descendant cannot become the parent of its ancestor")
    void preventCycle() throws Exception {
        Organization current = unit(10L, null, 1L, "NATIONAL");
        Organization child = unit(20L, 30L, 2L, "NORTH");
        Organization grandchild = unit(30L, 10L, 3L, "NORTH");

        when(organizationDAO.findByIdForUpdate(connection, 10L)).thenReturn(current);
        when(organizationDAO.findByIdForUpdate(connection, 20L)).thenReturn(child);
        when(organizationDAO.findByIdForUpdate(connection, 30L)).thenReturn(grandchild);

        OrganizationException error = assertThrows(OrganizationException.class,
                () -> service.updateUnit(10L,
                        new UnitInput("Kinh doanh", 20L, 1L, "NATIONAL", true)));

        assertEquals(ErrorCode.CYCLE, error.getCode());
        verify(organizationDAO, never()).update(eq(connection), anyLong(), anyString(),
                anyLong(), anyLong(), anyString(), anyBoolean());
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Changing leader assigns an unassigned active user to the unit")
    void assignLeader() throws Exception {
        Organization current = unit(10L, null, 1L, "NATIONAL");
        Organization updated = unit(10L, null, 5L, "NATIONAL");
        User newLeader = activeUser(5L, null);

        when(organizationDAO.findByIdForUpdate(connection, 10L)).thenReturn(current);
        when(organizationDAO.existsByNameExcluding(connection, "Kinh doanh", 10L)).thenReturn(false);
        when(userDAO.findByIdForUpdate(connection, 5L)).thenReturn(newLeader);
        when(organizationDAO.findUnitIdByLeader(connection, 5L)).thenReturn(null);
        when(organizationDAO.update(connection, 10L, "Kinh doanh", null, 5L,
                "NATIONAL", true)).thenReturn(1);
        when(userTeamDAO.assignUserToTeam(connection, 5L, 10L)).thenReturn(1);
        when(organizationDAO.findById(connection, 10L)).thenReturn(updated);

        Organization result = service.updateUnit(10L,
                new UnitInput("Kinh doanh", null, 5L, "NATIONAL", true));

        assertEquals(5L, result.getManagerId());
        verify(userTeamDAO).assignUserToTeam(connection, 5L, 10L);
        verify(connection).commit();
    }

    @Test
    @DisplayName("A user already belonging to another team cannot lead this unit")
    void enforceOneUserOneTeam() throws Exception {
        when(organizationDAO.findByIdForUpdate(connection, 10L))
                .thenReturn(unit(10L, null, 1L, "NATIONAL"));
        when(organizationDAO.existsByNameExcluding(connection, "Kinh doanh", 10L)).thenReturn(false);
        when(userDAO.findByIdForUpdate(connection, 5L)).thenReturn(activeUser(5L, 99L));
        when(organizationDAO.findUnitIdByLeader(connection, 5L)).thenReturn(null);

        OrganizationException error = assertThrows(OrganizationException.class,
                () -> service.updateUnit(10L,
                        new UnitInput("Kinh doanh", null, 5L, "NATIONAL", true)));

        assertEquals(ErrorCode.USER_ALREADY_IN_TEAM, error.getCode());
        verify(organizationDAO, never()).update(eq(connection), anyLong(), anyString(),
                eq(null), anyLong(), anyString(), anyBoolean());
        verify(userTeamDAO, never()).assignUserToTeam(eq(connection), anyLong(), anyLong());
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Region update accepts and persists an FE-supported region")
    void updateRegion() throws Exception {
        Organization current = unit(10L, null, 1L, "NATIONAL");
        Organization updated = unit(10L, null, 1L, "SOUTH");
        User leader = activeUser(1L, 10L);

        when(organizationDAO.findByIdForUpdate(connection, 10L)).thenReturn(current);
        when(organizationDAO.existsByNameExcluding(connection, "Kinh doanh", 10L)).thenReturn(false);
        when(userDAO.findByIdForUpdate(connection, 1L)).thenReturn(leader);
        when(organizationDAO.findUnitIdByLeader(connection, 1L)).thenReturn(10L);
        when(organizationDAO.update(connection, 10L, "Kinh doanh", null, 1L,
                "SOUTH", true)).thenReturn(1);
        when(organizationDAO.findById(connection, 10L)).thenReturn(updated);

        Organization result = service.updateUnit(10L,
                new UnitInput("Kinh doanh", null, 1L, "south", true));

        assertEquals("SOUTH", result.getRegion());
        verify(organizationDAO).update(connection, 10L, "Kinh doanh", null, 1L,
                "SOUTH", true);
    }

    @Test
    @DisplayName("Unsupported region is rejected before a transaction is opened")
    void rejectInvalidRegion() throws Exception {
        OrganizationException error = assertThrows(OrganizationException.class,
                () -> service.createUnit(
                        new UnitInput("Kinh doanh", null, 1L, "UNKNOWN", true)));

        assertEquals(ErrorCode.INVALID_REGION, error.getCode());
        verify(organizationDAO, never()).insert(eq(connection), anyString(), eq(null),
                anyLong(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Parent must exist")
    void rejectMissingParent() throws Exception {
        when(organizationDAO.findByIdForUpdate(connection, 404L)).thenReturn(null);

        OrganizationException error = assertThrows(OrganizationException.class,
                () -> service.createUnit(
                        new UnitInput("Kinh doanh", 404L, 1L, "NORTH", true)));

        assertEquals(ErrorCode.PARENT_NOT_FOUND, error.getCode());
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Leader must exist and be active")
    void rejectInactiveLeader() throws Exception {
        User inactive = activeUser(5L, null);
        inactive.setActive(false);

        when(organizationDAO.existsByNameExcluding(connection, "Kinh doanh", null)).thenReturn(false);
        when(userDAO.findByIdForUpdate(connection, 5L)).thenReturn(inactive);

        OrganizationException error = assertThrows(OrganizationException.class,
                () -> service.createUnit(
                        new UnitInput("Kinh doanh", null, 5L, "NORTH", true)));

        assertEquals(ErrorCode.INVALID_LEADER, error.getCode());
        verify(organizationDAO, never()).insert(eq(connection), anyString(), eq(null),
                anyLong(), anyString(), anyBoolean());
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Tree retrieval preserves the flat parent-linked contract consumed by FE")
    void treeRetrieval() throws Exception {
        Organization root = unit(1L, null, 10L, "NATIONAL");
        Organization child = unit(2L, 1L, 11L, "NORTH");
        when(organizationDAO.findAll(connection)).thenReturn(List.of(root, child));

        List<Organization> result = service.getUnits();

        assertEquals(2, result.size());
        assertNull(result.get(0).getParentId());
        assertEquals(1L, result.get(1).getParentId());
    }

    private User activeUser(long id, Long teamId) {
        User user = new User();
        user.setId(id);
        user.setTeamId(teamId);
        user.setActive(true);
        user.setStatus("ACTIVE");
        return user;
    }

    private Organization unit(long id, Long parentId, Long managerId, String region) {
        Organization unit = new Organization();
        unit.setId(id);
        unit.setName("Kinh doanh");
        unit.setParentId(parentId);
        unit.setManagerId(managerId);
        unit.setRegion(region);
        unit.setActive(true);
        return unit;
    }
}
