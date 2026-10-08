package com.crm.service.permissions;

import com.crm.config.DatabaseConfig;
import com.crm.service.auth.AuthService;
import com.crm.service.auth.LoginResult;
import com.crm.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataScopeIntegrationTest {
    private static final String PASSWORD = "Sprint1-Test-93";
    private final List<Long> users = new ArrayList<>();
    private final List<Long> teams = new ArrayList<>();
    private long salesA, salesB, otherSales, lead, director, admin;
    private long parentTeam, childTeam, otherTeam;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("CRM_DB_URL");
        assertNotNull(url, "Set CRM_DB_URL to the isolated test database");
        assertTrue(url.contains("/crm_sprint1_test"), "Tests must not use crm_db");
        parentTeam = team("Scope parent", null);
        childTeam = team("Scope child", parentTeam);
        otherTeam = team("Scope outside", null);
        salesA = user("sales-a", childTeam, "SALES_REP", "ALL");
        salesB = user("sales-b", parentTeam, "SALES_REP", "ALL");
        otherSales = user("sales-outside", otherTeam, "SALES_REP", "ALL");
        lead = user("lead", parentTeam, "TEAM_LEAD", "SELF");
        director = user("director", null, "DIRECTOR", "SELF");
        admin = user("admin", null, "ADMIN", "SELF");
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection c = DatabaseConfig.getConnection()) {
            for (Long id : users) {
                try (PreparedStatement s = c.prepareStatement("DELETE FROM customers WHERE owner_user_id = ?")) {
                    s.setLong(1, id); s.executeUpdate();
                }
                try (PreparedStatement s = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    s.setLong(1, id); s.executeUpdate();
                }
            }
            Collections.reverse(teams);
            for (Long id : teams) {
                try (PreparedStatement s = c.prepareStatement("DELETE FROM teams WHERE id = ?")) {
                    s.setLong(1, id); s.executeUpdate();
                }
            }
        }
    }

    @Test
    void loginAndModuleScopeUseRealRoleAndTeamRows() throws Exception {
        for (String identity : List.of("sales-a", "lead", "director", "admin")) {
            LoginResult result = new AuthService().login(email(identity), PASSWORD);
            assertEquals(LoginResult.Status.SUCCESS, result.getStatus(), identity);
        }
        DataScopeService service = new DataScopeService();
        DataScopeContext self = service.resolve(salesA, "customer", "read");
        assertEquals(ScopeType.SELF, self.scopeType());
        assertTrue(self.canAccessOwner(salesA));
        assertFalse(self.canAccessOwner(salesB));

        DataScopeContext team = service.resolve(lead, "customer", "read");
        assertEquals(ScopeType.TEAM, team.scopeType());
        assertTrue(team.canAccessOwner(salesA), "descendant team member");
        assertTrue(team.canAccessOwner(salesB), "same team member");
        assertFalse(team.canAccessOwner(otherSales), "outside team");

        assertEquals(ScopeType.ALL, service.resolve(director, "customer", "read").scopeType());
        assertEquals(ScopeType.ALL, service.resolve(admin, "customer", "read").scopeType());
        assertFalse(new PermissionService().hasPermission(salesA, "user.read"));
    }

    @Test
    void salesRepCannotReadCustomerOfOtherSalesRep() throws Exception {
        com.crm.service.customers.CustomerService customerService = new com.crm.service.customers.CustomerService();
        com.crm.dto.customers.CustomerWriteRequest reqB = new com.crm.dto.customers.CustomerWriteRequest();
        reqB.setName("Khách hàng riêng của Sales B");
        reqB.setStatus("TIEM_NANG");
        var customerOfB = customerService.create(salesB, reqB);
        long customerBId = (Long) customerOfB.get("id");

        // Sales B reads his own customer -> SUCCESS
        assertNotNull(customerService.getById(salesB, customerBId));

        // Sales A attempts to read Customer of Sales B -> SecurityException 403
        SecurityException ex = assertThrows(SecurityException.class, () -> customerService.getById(salesA, customerBId));
        assertEquals("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.", ex.getMessage());

        // Sales A searches customers -> Customer of B is not present in list
        var listForA = customerService.search(salesA, null, null, 1, 50);
        List<Map<String, Object>> itemsA = (List<Map<String, Object>>) listForA.get("items");
        boolean containsB = itemsA.stream().anyMatch(c -> Objects.equals(c.get("id"), customerBId));
        assertFalse(containsB, "Sales A must not see customer of Sales B in list");
    }

    @Test
    void teamLeadAndAdminRulesAreEnforcedBeforeReplacingRoles() throws Exception {
        long noTeam = user("without-team", null, "SALES_REP", "SELF");
        PermissionService service = new PermissionService();
        long leadRole = roleId("TEAM_LEAD");
        long salesRole = roleId("SALES_REP");
        IllegalArgumentException missingTeam = assertThrows(IllegalArgumentException.class,
                () -> service.assign(noTeam, List.of(leadRole), "TEAM", admin));
        assertEquals("Trưởng nhóm kinh doanh phải được gán vào một nhóm.", missingTeam.getMessage());

        IllegalArgumentException selfRemoval = assertThrows(IllegalArgumentException.class,
                () -> service.assign(admin, List.of(salesRole), "SELF", admin));
        assertEquals("Bạn không thể tự thu hồi quyền quản trị của chính mình.", selfRemoval.getMessage());
        IllegalArgumentException lastAdmin = assertThrows(IllegalArgumentException.class,
                () -> service.assign(admin, List.of(salesRole), "SELF", director));
        assertTrue(lastAdmin.getMessage().contains("cuối cùng"));
        assertTrue(new PermissionService().hasPermission(admin, "permission.manage"));
    }

    private long team(String name, Long parent) throws Exception {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO teams(name,active,parent_id) VALUES (?,TRUE,?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, name + "-" + UUID.randomUUID());
            if (parent == null) s.setNull(2, Types.BIGINT); else s.setLong(2, parent);
            s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) { rs.next(); long id = rs.getLong(1); teams.add(id); return id; }
        }
    }

    private long user(String key, Long teamId, String role, String legacyScope) throws Exception {
        long id;
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = c.prepareStatement("""
                INSERT INTO users(full_name,email,password_hash,status,team_id,data_scope)
                VALUES (?,?,?,'ACTIVE',?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, "Scope " + key); s.setString(2, email(key));
            s.setString(3, PasswordUtil.hash(PASSWORD));
            if (teamId == null) s.setNull(4, Types.BIGINT); else s.setLong(4, teamId);
            s.setString(5, legacyScope); s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) { rs.next(); id = rs.getLong(1); }
        }
        users.add(id);
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO user_roles(user_id,role_id) VALUES (?,?)")) {
            s.setLong(1, id); s.setLong(2, roleId(role)); s.executeUpdate();
        }
        return id;
    }

    private String email(String key) { return "scope-" + key + "@example.test"; }

    private long roleId(String code) throws Exception {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT id FROM roles WHERE code = ?")) {
            s.setString(1, code);
            try (ResultSet rs = s.executeQuery()) { assertTrue(rs.next(), code); return rs.getLong(1); }
        }
    }
}
