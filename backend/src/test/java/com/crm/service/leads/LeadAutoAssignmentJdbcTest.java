package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.controller.leads.LeadServlet;
import com.crm.dao.leads.AssignmentRuleDAO;
import com.crm.dao.leads.LeadAssignmentHistoryDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.AssignmentRule;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LeadAutoAssignmentJdbcTest {

    private Connection physical;

    private Connection borrowed() {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class}, (p, m, a) -> {
            if ("close".equals(m.getName())) return null;
            try {
                return m.invoke(physical, a);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private AssignmentRuleDAO ruleDAO;
    private LeadAssignmentHistoryDAO historyDAO;
    private LeadDAO leadDAO;
    private LeadAutoAssignmentService service;

    @BeforeEach
    void setup() throws Exception {
        assertTrue(String.valueOf(System.getenv("CRM_DB_URL")).contains("/crm_sprint1_test"), "Isolated test schema required");
        physical = DatabaseConfig.getConnection();

        try (var s = physical.createStatement()) {
            s.execute("CREATE TEMPORARY TABLE users (id BIGINT PRIMARY KEY, username VARCHAR(50), full_name VARCHAR(255), is_active BOOLEAN DEFAULT 1, status VARCHAR(30) DEFAULT 'ACTIVE', team_id BIGINT NULL) ENGINE=InnoDB");
            s.execute("INSERT INTO users(id,username,full_name,is_active) VALUES (101, 'rep1', 'Nguyễn Văn Hùng', 1), (102, 'rep2', 'Trần Thu Hà', 1), (103, 'lead1', 'Lê Trưởng Nhóm', 1)");

            s.execute("CREATE TEMPORARY TABLE roles (id BIGINT PRIMARY KEY, code VARCHAR(50), name VARCHAR(100)) ENGINE=InnoDB");
            s.execute("INSERT INTO roles VALUES (1, 'ADMIN', 'Admin'), (2, 'TEAM_LEAD', 'Trưởng nhóm'), (3, 'SALES_REP', 'Nhân viên')");

            s.execute("CREATE TEMPORARY TABLE permissions (id BIGINT PRIMARY KEY, code VARCHAR(50), name VARCHAR(100)) ENGINE=InnoDB");
            s.execute("INSERT INTO permissions VALUES (1, 'lead.assign', 'Phân bổ lead'), (2, 'lead.assignment_rules', 'Quy tắc'), (3, 'lead.auto_assign', 'Tự động')");

            s.execute("CREATE TEMPORARY TABLE role_permissions (role_id BIGINT, permission_id BIGINT, PRIMARY KEY(role_id, permission_id)) ENGINE=InnoDB");
            s.execute("INSERT INTO role_permissions VALUES (1, 1), (1, 2), (1, 3), (2, 1)");

            s.execute("CREATE TEMPORARY TABLE user_roles (user_id BIGINT, role_id BIGINT, PRIMARY KEY(user_id, role_id)) ENGINE=InnoDB");
            s.execute("INSERT INTO user_roles VALUES (103, 2), (101, 3), (102, 3)");

            s.execute("CREATE TEMPORARY TABLE leads (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "full_name VARCHAR(150) NOT NULL, " +
                    "company_name VARCHAR(255) NULL, " +
                    "email VARCHAR(255) NULL, " +
                    "phone VARCHAR(50) NULL, " +
                    "source VARCHAR(50) NULL DEFAULT 'WEB_FORM', " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'NEW', " +
                    "notes TEXT NULL, " +
                    "region VARCHAR(100) NULL, " +
                    "region_code VARCHAR(50) NULL, " +
                    "industry VARCHAR(100) NULL, " +
                    "assigned_to BIGINT NULL, assigned_to_user_id BIGINT NULL, assigned_at DATETIME NULL, sla_deadline DATETIME NULL, sla_total_minutes INT DEFAULT 120, accepted_at DATETIME NULL, rejected_at DATETIME NULL, sla_result VARCHAR(30) NULL, is_locked BOOLEAN DEFAULT 0, " +
                    "unassigned_reason VARCHAR(255) NULL, " +
                    "is_deleted TINYINT(1) NOT NULL DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB");

            s.execute("CREATE TEMPORARY TABLE lead_assignment_rules (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "rule_code VARCHAR(50) NULL, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "priority_order INT NOT NULL DEFAULT 1, " +
                    "region VARCHAR(50) NOT NULL DEFAULT 'NATIONWIDE', " +
                    "industry VARCHAR(100) NOT NULL DEFAULT 'ALL', " +
                    "mechanism VARCHAR(50) NOT NULL DEFAULT 'ROUND_ROBIN', " +
                    "reps TEXT NULL, " +
                    "description TEXT NULL, " +
                    "is_active TINYINT(1) NOT NULL DEFAULT 1, " +
                    "total_assigned INT NOT NULL DEFAULT 0, " +
                    "last_assigned_index INT NOT NULL DEFAULT 0, " +
                    "last_assigned_user_id BIGINT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");

            s.execute("CREATE TEMPORARY TABLE lead_assignment_history (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "lead_id BIGINT NULL, " +
                    "lead_name VARCHAR(150) NOT NULL, " +
                    "lead_phone VARCHAR(50) NULL, " +
                    "company_name VARCHAR(255) NULL, " +
                    "rep_id VARCHAR(50) NOT NULL, " +
                    "rep_name VARCHAR(150) NOT NULL, " +
                    "rep_dept VARCHAR(150) NULL, " +
                    "method VARCHAR(255) NOT NULL, " +
                    "note TEXT NULL, " +
                    "assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
        }

        ruleDAO = new AssignmentRuleDAO() {
            @Override
            public List<AssignmentRule> findActiveOrderedByPriority() {
                try (PreparedStatement ps = borrowed().prepareStatement("SELECT * FROM lead_assignment_rules WHERE is_active = 1 ORDER BY priority_order ASC, id ASC");
                     ResultSet rs = ps.executeQuery()) {
                    List<AssignmentRule> list = new java.util.ArrayList<>();
                    while (rs.next()) {
                        AssignmentRule r = new AssignmentRule();
                        r.setId(rs.getLong("id"));
                        r.setName(rs.getString("name"));
                        r.setPriority(rs.getInt("priority_order"));
                        r.setRegion(rs.getString("region"));
                        r.setIndustry(rs.getString("industry"));
                        r.setMechanism(rs.getString("mechanism"));
                        r.setReps(rs.getString("reps") == null ? List.of() : com.google.gson.JsonParser.parseString(rs.getString("reps")).getAsJsonArray().asList().stream().map(com.google.gson.JsonElement::getAsString).toList());
                        r.setLastAssignedIndex(rs.getInt("last_assigned_index"));
                        list.add(r);
                    }
                    return list;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        leadDAO = new LeadDAO() {
            @Override
            public List<SalesRepDTO> getSalesRepsWithWorkload() {
                SalesRepDTO r1 = new SalesRepDTO("101", "Nguyễn Văn Hùng", "NH", "Sales", "Phòng Kinh Doanh", "NORTH", 2, 20, true);
                SalesRepDTO r2 = new SalesRepDTO("102", "Trần Thu Hà", "TH", "Sales", "Phòng Kinh Doanh", "NORTH", 5, 20, true);
                return List.of(r1, r2);
            }

            @Override
            public Lead findById(long id) {
                try (PreparedStatement ps = borrowed().prepareStatement("SELECT * FROM leads WHERE id = ?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Lead l = new Lead();
                            l.setId(rs.getLong("id"));
                            l.setFullName(rs.getString("full_name"));
                            l.setPhone(rs.getString("phone"));
                            l.setCompanyName(rs.getString("company_name"));
                            l.setStatus(rs.getString("status"));
                            l.setRegion(rs.getString("region"));
                            l.setRegionCode(rs.getString("region_code"));
                            l.setIndustry(rs.getString("industry"));
                            l.setAssignedTo(rs.getObject("assigned_to", Long.class));
                            l.setUnassignedReason(rs.getString("unassigned_reason"));
                            return l;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            public List<Lead> findUnassignedOrPendingQueue() {
                try (PreparedStatement ps = borrowed().prepareStatement("SELECT * FROM leads WHERE status IN ('NEW', 'PENDING_ASSIGNMENT') AND (assigned_to IS NULL OR assigned_to = 0)");
                     ResultSet rs = ps.executeQuery()) {
                    List<Lead> list = new java.util.ArrayList<>();
                    while (rs.next()) {
                        Lead l = new Lead();
                        l.setId(rs.getLong("id"));
                        l.setFullName(rs.getString("full_name"));
                        l.setStatus(rs.getString("status"));
                        l.setRegion(rs.getString("region"));
                        l.setRegionCode(rs.getString("region_code"));
                        l.setIndustry(rs.getString("industry"));
                        list.add(l);
                    }
                    return list;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        historyDAO = new LeadAssignmentHistoryDAO();

        service = new LeadAutoAssignmentService(ruleDAO, historyDAO, leadDAO) {
            @Override
            protected Connection open() {
                return borrowed();
            }
        };
    }

    @AfterEach
    void tearDown() throws Exception {
        if (physical != null && !physical.isClosed()) {
            physical.close();
        }
    }

    private LeadDAO productionDao() {
        return new LeadDAO() { @Override public Connection open() { return borrowed(); } };
    }

    @Test
    void productionAssignmentInitializesSlaAndCannotOverwriteAcceptedOrConvertedLead() throws Exception {
        try(var s=physical.createStatement()) {
            s.execute("INSERT INTO leads(id,full_name,status) VALUES(800,'QA lifecycle','NEW'),(801,'QA converted','CONVERTED')");
        }
        var real=productionDao();
        assertTrue(real.updateAssignment(borrowed(),800,101L,"PENDING_RESPONSE",null));
        try(var s=physical.createStatement();var r=s.executeQuery("SELECT assigned_to,assigned_to_user_id,status,assigned_at,sla_deadline FROM leads WHERE id=800")) {
            assertTrue(r.next());assertEquals(101,r.getLong(1));assertEquals(101,r.getLong(2));assertEquals("PENDING_RESPONSE",r.getString(3));
            assertEquals(120*60_000L,r.getTimestamp(5).getTime()-r.getTimestamp(4).getTime());
        }
        assertFalse(real.updateAssignment(borrowed(),800,102L,"PENDING_RESPONSE",null));
        assertFalse(real.updateAssignment(borrowed(),801,102L,"PENDING_RESPONSE",null));
        assertFalse(real.findUnassignedOrPendingQueue().stream().anyMatch(l->l.getId()==800L||l.getId()==801L));
    }

    @Test
    void realSalesCatalogExcludesInventedInactiveAndNonSalesUsersAndCountsLegacyIds() throws Exception {
        try(var s=physical.createStatement()) {
            s.execute("CREATE TEMPORARY TABLE teams(id BIGINT PRIMARY KEY,name VARCHAR(100))");
            s.execute("UPDATE users SET status='INACTIVE' WHERE id=103");
            s.execute("INSERT INTO users(id,username,full_name) VALUES(104,'outsider','Not a sales user')");
            s.execute("ALTER TABLE leads MODIFY assigned_to VARCHAR(50) NULL");
            s.execute("INSERT INTO leads(full_name,assigned_to,status) VALUES('Legacy','USR-101','PENDING_RESPONSE'),('Malformed','not-an-id','PENDING_RESPONSE'),('Closed','101','CONVERTED')");
        }
        var reps=productionDao().getSalesRepsWithWorkload();
        assertEquals(List.of("101","102"),reps.stream().map(SalesRepDTO::getId).toList());
        assertEquals(1,reps.get(0).getCurrentLeads());assertEquals(0,reps.get(1).getCurrentLeads());
    }

    @Test
    void missingConfiguredRepDoesNotFallBackToUnrelatedSalesUser() throws Exception {
        try(var s=physical.createStatement()) {
            s.execute("INSERT INTO lead_assignment_rules(id,name,reps) VALUES(1,'Missing rep','[\"999999\"]')");
            s.execute("INSERT INTO leads(id,full_name,status) VALUES(900,'No accidental assignment','NEW')");
        }
        assertFalse(service.assignLead(leadDAO.findById(900)));
        try(var s=physical.createStatement();var r=s.executeQuery("SELECT assigned_to FROM leads WHERE id=900")){assertTrue(r.next());assertNull(r.getObject(1));}
        assertThrows(IllegalArgumentException.class,()->service.assignLeadManual(900,"bogus-101","QA invalid ID"));
    }

    @Test
    void testFirstMatchWinsEvaluatesHighestPriorityRule() throws Exception {
        try (var s = physical.createStatement()) {
            s.execute("INSERT INTO lead_assignment_rules (id, name, priority_order, region, industry, mechanism, reps, is_active) " +
                    "VALUES (1, 'Rule Priority 1 (North)', 1, 'NORTH', 'ALL', 'ROUND_ROBIN', '[\"101\"]', 1), " +
                    "(2, 'Rule Priority 2 (Nationwide)', 2, 'NATIONWIDE', 'ALL', 'ROUND_ROBIN', '[\"102\"]', 1)");
            s.execute("INSERT INTO leads (id, full_name, region, region_code, industry, status) " +
                    "VALUES (10, 'Nguyễn Văn Test', 'Miền Bắc', 'NORTH', 'Bất động sản', 'NEW')");
        }

        Lead lead = leadDAO.findById(10);
        boolean assigned = service.assignLead(lead);

        assertTrue(assigned, "Lead should be assigned");
        assertEquals(101L, lead.getAssignedTo(), "First match (priority 1) must win");
        assertEquals("PENDING_RESPONSE", lead.getStatus());
    }

    @Test
    void testRoundRobinDistributionRotatesSequentially() throws Exception {
        try (var s = physical.createStatement()) {
            s.execute("INSERT INTO lead_assignment_rules (id, name, priority_order, region, industry, mechanism, reps, is_active, last_assigned_index) " +
                    "VALUES (1, 'Round Robin Rule', 1, 'NATIONWIDE', 'ALL', 'ROUND_ROBIN', '[\"101\", \"102\"]', 1, 0)");
            s.execute("INSERT INTO leads (id, full_name, region_code, status) VALUES (1, 'Lead 1', 'NORTH', 'NEW'), (2, 'Lead 2', 'SOUTH', 'NEW')");
        }

        Lead l1 = leadDAO.findById(1);
        Lead l2 = leadDAO.findById(2);

        assertTrue(service.assignLead(l1));
        assertEquals(101L, l1.getAssignedTo(), "First lead gets index 0 (101)");

        // Check rule pointer updated in DB
        try (var ps = physical.prepareStatement("SELECT last_assigned_index FROM lead_assignment_rules WHERE id = 1");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Pointer must be incremented to 1");
        }

        assertTrue(service.assignLead(l2));
        assertEquals(102L, l2.getAssignedTo(), "Second lead gets index 1 (102)");
    }

    @Test
    void testUnassignedQueueFallbackWhenNoRulesMatch() throws Exception {
        try (var s = physical.createStatement()) {
            s.execute("INSERT INTO lead_assignment_rules (id, name, priority_order, region, industry, mechanism, is_active) " +
                    "VALUES (1, 'Strict North Rule', 1, 'NORTH', 'Bất động sản', 'ROUND_ROBIN', 1)");
            s.execute("INSERT INTO leads (id, full_name, region, region_code, industry, status) " +
                    "VALUES (5, 'Khách Miền Trung Ngành Y Tế', 'Miền Trung', 'CENTRAL', 'Y tế', 'NEW')");
        }

        Lead lead = leadDAO.findById(5);
        boolean assigned = service.assignLead(lead);

        assertFalse(assigned, "Lead with no matching rule must fall back to queue");
        assertEquals("PENDING_ASSIGNMENT", lead.getStatus());
        assertNull(lead.getAssignedTo());
        assertNotNull(lead.getUnassignedReason());
    }

    @Test
    void testManualAssignmentUpdatesLeadAndLogsAuditHistoryTransactionally() throws Exception {
        try (var s = physical.createStatement()) {
            s.execute("INSERT INTO leads (id, full_name, phone, company_name, status) " +
                    "VALUES (20, 'Đỗ Khách VIP', '0912345678', 'Công ty ABC', 'PENDING_ASSIGNMENT')");
        }

        boolean assigned = service.assignLeadManual(20, "102", "Trưởng nhóm giao việc gấp");
        assertTrue(assigned, "Manual assignment should succeed");

        Lead updated = leadDAO.findById(20);
        assertEquals(102L, updated.getAssignedTo());
        assertEquals("PENDING_RESPONSE", updated.getStatus());

        // Check history table
        try (var ps = physical.prepareStatement("SELECT * FROM lead_assignment_history WHERE lead_id = 20");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next(), "Audit log must be created");
            assertEquals("102", rs.getString("rep_id"));
            assertEquals("Thủ công: Trưởng nhóm phân công", rs.getString("method"));
            assertTrue(rs.getString("note").contains("gấp"));
        }
    }

    @Test
    void testAtomicTransactionRollsBackOnFailure() throws Exception {
        try (var s = physical.createStatement()) {
            s.execute("INSERT INTO leads (id, full_name, status) VALUES (30, 'Lead Transaction Test', 'PENDING_ASSIGNMENT')");
        }

        LeadAutoAssignmentService faultyService = new LeadAutoAssignmentService(ruleDAO, new LeadAssignmentHistoryDAO() {
            @Override
            public long insert(Connection conn, AssignmentHistory history) {
                throw new RuntimeException("Simulated history insert DB crash");
            }
        }, leadDAO) {
            @Override
            protected Connection open() {
                return borrowed();
            }
        };

        assertThrows(RuntimeException.class, () -> faultyService.assignLeadManual(30, "101", "Note"));

        // Verify lead was NOT updated due to rollback!
        Lead lead = leadDAO.findById(30);
        assertEquals("PENDING_ASSIGNMENT", lead.getStatus(), "Status should remain PENDING_ASSIGNMENT after rollback");
        assertNull(lead.getAssignedTo(), "Assigned_to should remain null after rollback");
    }

    @Test
    void testBackgroundSchedulerProcessesPendingQueue() throws Exception {
        try (var s = physical.createStatement()) {
            s.execute("INSERT INTO lead_assignment_rules (id, name, priority_order, region, industry, mechanism, reps, is_active) " +
                    "VALUES (1, 'Nationwide Rule', 1, 'NATIONWIDE', 'ALL', 'ROUND_ROBIN', '[\"101\"]', 1)");
            s.execute("INSERT INTO leads (id, full_name, region_code, status) VALUES (50, 'Queue Lead 1', 'NORTH', 'PENDING_ASSIGNMENT')");
        }

        int count = service.processPendingQueue();
        assertEquals(1, count, "Pending queue processing should assign 1 lead");

        Lead lead = leadDAO.findById(50);
        assertEquals("PENDING_RESPONSE", lead.getStatus());
        assertEquals(101L, lead.getAssignedTo());
    }

    @Test
    void testServletEnforcesLeadAssignPermissionAndRejectsUnauthorized() throws Exception {
        AuthorizationService mockAuth = new AuthorizationService() {
            @Override
            public void requirePermission(HttpServletRequest request, String permission) {
                Long uid = (Long) request.getSession().getAttribute("userId");
                if (uid == null) throw new SecurityException("Phiên đăng nhập không hợp lệ");
                if (uid == 101L && "lead.assign".equals(permission)) {
                    throw new SecurityException("Bạn không có quyền phân bổ Lead.");
                }
            }
        };

        LeadServlet servlet = new LeadServlet(null, service, leadDAO, historyDAO, mockAuth);

        // Case 1: Anonymous -> 401
        HttpServletRequest reqAnon = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class}, (p, m, a) -> {
            if ("getMethod".equals(m.getName())) return "POST";
            if ("getPathInfo".equals(m.getName())) return "/assign";
            if ("getSession".equals(m.getName())) {
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class}, (p2, m2, a2) -> null);
            }
            if ("getReader".equals(m.getName())) return new BufferedReader(new StringReader("{\"leadId\":\"20\",\"repId\":\"101\"}"));
            return null;
        });

        StringWriter sw = new StringWriter();
        int[] status = new int[1];
        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class}, (p, m, a) -> {
            if ("setStatus".equals(m.getName())) status[0] = (int) a[0];
            if ("getWriter".equals(m.getName())) return new PrintWriter(sw);
            return null;
        });

        servlet.service(reqAnon, res);
        assertEquals(401, status[0], "Anonymous call to /assign must return 401");

        // Case 2: Unauthorized Sales Rep (101) lacking lead.assign -> 403
        HttpServletRequest reqRep = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class}, (p, m, a) -> {
            if ("getMethod".equals(m.getName())) return "POST";
            if ("getPathInfo".equals(m.getName())) return "/assign";
            if ("getSession".equals(m.getName())) {
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class}, (p2, m2, a2) -> {
                    if ("getAttribute".equals(m2.getName()) && "userId".equals(a2[0])) return 101L;
                    return null;
                });
            }
            if ("getReader".equals(m.getName())) return new BufferedReader(new StringReader("{\"leadId\":\"20\",\"repId\":\"101\"}"));
            return null;
        });

        status[0] = 0;
        servlet.service(reqRep, res);
        assertEquals(403, status[0], "Sales rep lacking lead.assign must return 403");
    }
}
