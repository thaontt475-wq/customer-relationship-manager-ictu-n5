package com.crm.dao.audit;

import com.crm.model.AuditLogFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogDAOTest {
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    @Test
    void findAppliesActorObjectTypeAndTimeRangeFilters() throws Exception {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sql.capture())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Timestamp from = Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 0, 0));
        Timestamp to = Timestamp.valueOf(LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        AuditLogFilter filter = new AuditLogFilter();
        filter.setUserId(7L);
        filter.setObjectType("OPPORTUNITY");
        filter.setFrom(from);
        filter.setTo(to);
        filter.setLimit(100);

        new AuditLogDAO().find(connection, filter);

        assertTrue(sql.getValue().contains("actor_user_id = ?"));
        assertTrue(sql.getValue().contains("object_type = ?"));
        assertTrue(sql.getValue().contains("created_at >= ?"));
        assertTrue(sql.getValue().contains("created_at <= ?"));
        verify(statement).setLong(1, 7L);
        verify(statement).setString(2, "OPPORTUNITY");
        verify(statement).setTimestamp(3, from);
        verify(statement).setTimestamp(4, to);
        verify(statement).setInt(5, 100);
    }
}
