package com.crm.controller.dashboard;

import com.crm.model.Team;
import com.crm.model.User;
import com.crm.service.teams.TeamService;
import com.crm.service.users.UserService;
import com.crm.util.SessionKey;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServletTest {
    private UserService userService;
    private TeamService teamService;
    private DashboardServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        userService = mock(UserService.class);
        teamService = mock(TeamService.class);
        servlet = new DashboardServlet();
        inject(servlet, "userService", userService);
        inject(servlet, "teamService", teamService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Admin", "Director"})
    void adminAndDirectorReceiveManagementDashboard(String role) throws Exception {
        DashboardRequest fixture = requestFor(role);
        when(userService.searchUsers(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(page(12));
        when(userService.searchUsers(isNull(), isNull(), org.mockito.ArgumentMatchers.eq("ACTIVE"),
                anyInt(), anyInt())).thenReturn(page(9));
        when(userService.searchUsers(isNull(), isNull(), org.mockito.ArgumentMatchers.eq("LOCKED"),
                anyInt(), anyInt())).thenReturn(page(3));
        when(teamService.findAllTeams()).thenReturn(List.of(new Team(), new Team()));

        servlet.doGet(fixture.request(), fixture.response());

        verify(fixture.request()).setAttribute("canManageUsers", true);
        verify(fixture.request()).setAttribute("totalUsers", 12L);
        verify(fixture.request()).setAttribute("activeUsers", 9L);
        verify(fixture.request()).setAttribute("lockedUsers", 3L);
        verify(fixture.request()).setAttribute("totalTeams", 2);
        verify(fixture.dispatcher()).forward(fixture.request(), fixture.response());
    }

    @Test
    void regularUserReceivesDashboardWithoutAdministrativeStatistics() throws Exception {
        DashboardRequest fixture = requestFor("Sales Rep");

        servlet.doGet(fixture.request(), fixture.response());

        verify(fixture.request()).setAttribute("canManageUsers", false);
        verify(userService, never()).searchUsers(any(), any(), any(), anyInt(), anyInt());
        verify(teamService, never()).findAllTeams();
        verify(fixture.dispatcher()).forward(fixture.request(), fixture.response());
    }

    private DashboardRequest requestFor(String role) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        User user = new User();
        user.setId(2101L);
        user.setDisplayName("CRM User");

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(2101L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of(role));
        when(userService.findById(2101L)).thenReturn(user);
        when(request.getRequestDispatcher("/jsp/dashboard/dashboard.jsp")).thenReturn(dispatcher);

        return new DashboardRequest(request, response, dispatcher);
    }

    private static UserService.UserPage page(long totalItems) {
        return new UserService.UserPage(List.of(), 1, 1, totalItems, totalItems == 0 ? 0 : totalItems);
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record DashboardRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            RequestDispatcher dispatcher) {
    }
}
