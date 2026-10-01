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
import static org.mockito.Mockito.verify;
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
        filter.setLimit(5_000);

        service.findLogs(filter);

        ArgumentCaptor<AuditLogFilter> captor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(auditLogDAO).find(eq(connection), captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals("OPPORTUNITY", captor.getValue().getObjectType());
        assertEquals(17L, captor.getValue().getObjectId());
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
