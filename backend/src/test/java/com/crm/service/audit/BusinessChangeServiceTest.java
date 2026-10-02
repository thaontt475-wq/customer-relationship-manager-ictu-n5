package com.crm.service.audit;

import com.crm.dao.audit.BusinessChangeDAO;
import com.crm.dao.users.UserDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessChangeServiceTest {

    @Test
    @DisplayName("Audit failure rolls back target write on same connection")
    void auditFailureRollsBackTargetWriteOnSameConnection() throws Exception {
        Connection c = mock(Connection.class);
        BusinessChangeDAO dao = mock(BusinessChangeDAO.class);
        AuditLogService audit = mock(AuditLogService.class);

        try (var construction = mockConstruction(UserDAO.class, (m, ctx) ->
                when(m.findRoleNamesByUserId(c, 10)).thenReturn(List.of("Admin")))) {

            LocalDate month = LocalDate.of(2026, 10, 1);
            BigDecimal amount = new BigDecimal("100");
            when(dao.saveTarget(c, null, 20, month, amount)).thenReturn(7L);
            when(audit.recordTargetChange(c, 10, "SALES_TARGET", 7, null, amount))
                    .thenThrow(new SQLException("audit unavailable"));

            BusinessChangeService service = new BusinessChangeService(dao, audit, () -> c);

            assertThrows(SQLException.class, () -> service.setTarget(10, 20, month, amount));
            verify(dao).saveTarget(c, null, 20, month, amount);
            verify(c).rollback();
            verify(c, never()).commit();
        }
    }

    @Test
    @DisplayName("Target writes and audit log commit together on success")
    void targetWritesAndAuditCommitTogether() throws Exception {
        Connection c = mock(Connection.class);
        BusinessChangeDAO dao = mock(BusinessChangeDAO.class);
        AuditLogService audit = mock(AuditLogService.class);

        try (var construction = mockConstruction(UserDAO.class, (m, ctx) ->
                when(m.findRoleNamesByUserId(c, 10)).thenReturn(List.of("Director")))) {

            LocalDate month = LocalDate.of(2026, 10, 1);
            BigDecimal amount = new BigDecimal("100");
            when(dao.saveTarget(c, null, 20, month, amount)).thenReturn(7L);

            BusinessChangeService service = new BusinessChangeService(dao, audit, () -> c);
            service.setTarget(10, 20, month, amount);

            var order = inOrder(dao, audit, c);
            order.verify(dao).saveTarget(c, null, 20, month, amount);
            order.verify(audit).recordTargetChange(c, 10, "SALES_TARGET", 7, null, amount);
            order.verify(c).commit();
        }
    }

    @Test
    @DisplayName("Sales Rep cannot set sales targets (SecurityException)")
    void salesRepCannotSetTargets() throws Exception {
        Connection c = mock(Connection.class);
        BusinessChangeDAO dao = mock(BusinessChangeDAO.class);
        AuditLogService audit = mock(AuditLogService.class);

        try (var construction = mockConstruction(UserDAO.class, (m, ctx) ->
                when(m.findRoleNamesByUserId(c, 10)).thenReturn(List.of("Sales Rep")))) {

            BusinessChangeService service = new BusinessChangeService(dao, audit, () -> c);

            assertThrows(SecurityException.class, () ->
                    service.setTarget(10, 20, LocalDate.of(2026, 10, 1), BigDecimal.ONE));
            verifyNoInteractions(dao, audit);
        }
    }
}
