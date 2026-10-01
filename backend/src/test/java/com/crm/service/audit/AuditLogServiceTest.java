package com.crm.service.audit;

import com.crm.dao.audit.AuditLogDAO;
import com.crm.model.AuditLog;
import com.crm.model.AuditLogFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {
    @Mock private AuditLogDAO auditLogDAO;
    @Mock private AuditLogService.ConnectionProvider connectionProvider;
    @Mock private Connection connection;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogDAO, connectionProvider);
    }

    @Test
    void recordRoleChangeStoresActorObjectAndJsonSnapshotsInCurrentTransaction() throws SQLException {
        when(auditLogDAO.insert(eq(connection), any(AuditLog.class))).thenReturn(91L);

        long id = service.recordRoleChange(
                connection,
                7L,
                42L,
                Map.of("roleIds", List.of(1L)),
                Map.of("roleIds", List.of(1L, 2L))
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDAO).insert(eq(connection), captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(91L, id);
        assertEquals(7L, saved.getActorUserId());
        assertEquals(AuditLogService.ACTION_ROLE_CHANGED, saved.getAction());
        assertEquals("USER", saved.getObjectType());
        assertEquals(42L, saved.getObjectId());
        assertEquals(1, saved.getBeforeValue().getAsJsonObject().getAsJsonArray("roleIds").size());
        assertEquals(2, saved.getAfterValue().getAsJsonObject().getAsJsonArray("roleIds").size());
    }

    @Test
    void recordDiscountChangeUsesCallerTransactionAndKeepsBeforeAfterValues() throws SQLException {
        when(auditLogDAO.insert(eq(connection), any(AuditLog.class))).thenReturn(92L);

        long id = service.recordDiscountChange(
                connection,
                7L,
                "QUOTE",
                51L,
                Map.of("discountPercent", 5, "amount", 950_000),
                Map.of("discountPercent", 10, "amount", 900_000)
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDAO).insert(eq(connection), captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(92L, id);
        assertEquals(7L, saved.getActorUserId());
        assertEquals(AuditLogService.ACTION_DISCOUNT_CHANGED, saved.getAction());
        assertEquals("QUOTE", saved.getObjectType());
        assertEquals(51L, saved.getObjectId());
        assertEquals(5, saved.getBeforeValue().getAsJsonObject().get("discountPercent").getAsInt());
        assertEquals(950_000, saved.getBeforeValue().getAsJsonObject().get("amount").getAsInt());
        assertEquals(10, saved.getAfterValue().getAsJsonObject().get("discountPercent").getAsInt());
        assertEquals(900_000, saved.getAfterValue().getAsJsonObject().get("amount").getAsInt());
        verifyNoInteractions(connectionProvider);
        verify(connection, never()).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void recordTargetChangeUsesCallerTransactionAndKeepsBeforeAfterValues() throws SQLException {
        when(auditLogDAO.insert(eq(connection), any(AuditLog.class))).thenReturn(93L);

        long id = service.recordTargetChange(
                connection,
                8L,
                "SALES_TARGET",
                61L,
                Map.of("quota", 100_000_000L, "period", "2026-Q3"),
                Map.of("quota", 120_000_000L, "period", "2026-Q3")
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDAO).insert(eq(connection), captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(93L, id);
        assertEquals(8L, saved.getActorUserId());
        assertEquals(AuditLogService.ACTION_TARGET_CHANGED, saved.getAction());
        assertEquals("SALES_TARGET", saved.getObjectType());
        assertEquals(61L, saved.getObjectId());
        assertEquals(100_000_000L, saved.getBeforeValue().getAsJsonObject().get("quota").getAsLong());
        assertEquals(120_000_000L, saved.getAfterValue().getAsJsonObject().get("quota").getAsLong());
        assertEquals("2026-Q3", saved.getAfterValue().getAsJsonObject().get("period").getAsString());
        verifyNoInteractions(connectionProvider);
        verify(connection, never()).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void auditInsertFailurePropagatesSoOwningBusinessTransactionCanRollback() throws SQLException {
        SQLException failure = new SQLException("audit insert failed");
        when(auditLogDAO.insert(eq(connection), any(AuditLog.class))).thenThrow(failure);

        SQLException thrown = assertThrows(SQLException.class, () -> service.recordDiscountChange(
                connection, 7L, "QUOTE", 51L,
                Map.of("discountPercent", 5), Map.of("discountPercent", 10)));

        assertEquals(failure, thrown);
        verifyNoInteractions(connectionProvider);
        verify(connection, never()).commit();
    }

    @Test
    void recordChangeRejectsInvalidRequiredFieldsBeforeDaoCall() {
        assertThrows(IllegalArgumentException.class,
                () -> service.recordChange(connection, 0L, "ROLE_CHANGED", "USER", 1L, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordChange(connection, 1L, "bad action!", "USER", 1L, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordChange(connection, 1L, "ROLE_CHANGED", "USER", 0L, null, null));
    }

    @Test
    void findLogsNormalizesFiltersAndCapsLimit() throws SQLException {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(auditLogDAO.find(eq(connection), any(AuditLogFilter.class))).thenReturn(List.of());

        AuditLogFilter filter = new AuditLogFilter();
        filter.setUserId(9L);
        filter.setObjectType(" opportunity ");
        filter.setObjectId(17L);
        filter.setFrom(Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 0, 0)));
        filter.setTo(Timestamp.valueOf(LocalDateTime.of(2026, 10, 31, 23, 59)));
        filter.setLimit(5_000);

        service.findLogs(filter);

        ArgumentCaptor<AuditLogFilter> captor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(auditLogDAO).find(eq(connection), captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals("OPPORTUNITY", captor.getValue().getObjectType());
        assertEquals(17L, captor.getValue().getObjectId());
        assertEquals(filter.getFrom(), captor.getValue().getFrom());
        assertEquals(filter.getTo(), captor.getValue().getTo());
        assertEquals(AuditLogFilter.MAX_LIMIT, captor.getValue().getLimit());
    }

    @Test
    void findLogsRejectsInvertedTimeRange() {
        AuditLogFilter filter = new AuditLogFilter();
        filter.setFrom(Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 0, 0)));
        filter.setTo(Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 0, 0)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.findLogs(filter)
        );

        assertTrue(error.getMessage().contains("from"));
    }
}
