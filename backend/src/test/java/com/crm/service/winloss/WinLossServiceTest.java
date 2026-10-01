package com.crm.service.winloss;

import com.crm.dao.winloss.WinLossDAO;
import com.crm.model.Competitor;
import com.crm.model.WinLossReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WinLossServiceTest {
    @Mock WinLossDAO dao;
    @Mock Connection connection;
    private WinLossService service;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(connection.getAutoCommit()).thenReturn(true);
        service = new WinLossService(dao, () -> connection);
    }

    @Test
    void crudWinReason() throws Exception {
        exerciseReasonCrud("WIN", 11L);
    }

    @Test
    void crudLossReason() throws Exception {
        exerciseReasonCrud("LOSS", 12L);
    }

    @Test
    void getReasonsKeepsWinAndLossSeparateAndExcludesInactiveByDefault() throws Exception {
        WinLossReason win = reason("WIN", "Giá cạnh tranh");
        WinLossReason loss = reason("LOSS", "Giá cạnh tranh");
        when(dao.findReasons(connection, "WIN", true)).thenReturn(List.of(win));
        when(dao.findReasons(connection, "LOSS", true)).thenReturn(List.of(loss));

        WinLossService.ReasonsResult result = service.getReasons(false);

        assertEquals(List.of(win), result.winReasons());
        assertEquals(List.of(loss), result.lossReasons());
    }

    @Test
    void updateCannotMoveReasonBetweenWinAndLossGroups() throws Exception {
        WinLossReason existing = reason("WIN", "Giá cạnh tranh");
        existing.setId(11L);
        WinLossReason submitted = reason("LOSS", "Giá cạnh tranh");
        submitted.setId(11L);
        when(dao.findReasonById(connection, 11L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateReason(submitted));

        verify(dao, never()).updateReason(any(), any());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void crudCompetitor() throws Exception {
        Competitor competitor = competitor("Apex CRM");
        when(dao.nextCompetitorOrder(connection)).thenReturn(1);
        when(dao.insertCompetitor(connection, competitor)).thenReturn(20L);
        when(dao.findCompetitorById(connection, 20L)).thenReturn(competitor);

        assertSame(competitor, service.createCompetitor(competitor));
        assertEquals(20L, competitor.getId());
        assertSame(competitor, service.updateCompetitor(competitor));
        assertEquals(WinLossService.DeleteOutcome.DELETED, service.deleteCompetitor(20L));

        verify(dao).insertCompetitor(connection, competitor);
        verify(dao).updateCompetitor(connection, competitor);
        verify(dao).deleteCompetitor(connection, 20L);
    }

    @Test
    void duplicateReasonIsRejectedWithinSameType() throws Exception {
        WinLossReason reason = reason("WIN", "Giá cạnh tranh");
        when(dao.reasonExists(connection, "WIN", "Giá cạnh tranh", null)).thenReturn(true);

        assertThrows(WinLossService.DuplicateException.class, () -> service.createReason(reason));

        verify(dao, never()).insertReason(any(), any());
        verify(connection).rollback();
    }

    @Test
    void duplicateCompetitorNameIsRejected() throws Exception {
        Competitor competitor = competitor("Apex CRM");
        when(dao.competitorExists(connection, "Apex CRM", null)).thenReturn(true);

        assertThrows(WinLossService.DuplicateException.class,
                () -> service.createCompetitor(competitor));
    }

    @Test
    void invalidReasonPayloadIsRejected() {
        WinLossReason reason = reason("OTHER", "");
        assertThrows(IllegalArgumentException.class, () -> service.createReason(reason));
    }

    @Test
    void invalidCompetitorWebsiteIsRejected() {
        Competitor competitor = competitor("Apex CRM");
        competitor.setWebsite("javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () -> service.createCompetitor(competitor));
    }

    @Test
    void inUseReasonIsDeactivatedWhenFkBlocksDelete() throws Exception {
        WinLossReason reason = reason("LOSS", "Đối thủ tốt hơn");
        reason.setId(30L);
        when(dao.findReasonById(connection, 30L)).thenReturn(reason);
        when(dao.deleteReason(connection, 30L)).thenThrow(new SQLException("FK", "23000"));

        assertEquals(WinLossService.DeleteOutcome.DEACTIVATED, service.deleteReason(30L));

        verify(dao).deactivateReason(connection, 30L);
        verify(connection).commit();
    }

    @Test
    void inUseCompetitorIsDeactivatedWhenFkBlocksDelete() throws Exception {
        Competitor competitor = competitor("Apex CRM");
        competitor.setId(31L);
        when(dao.findCompetitorById(connection, 31L)).thenReturn(competitor);
        when(dao.deleteCompetitor(connection, 31L)).thenThrow(new SQLException("FK", "23000"));

        assertEquals(WinLossService.DeleteOutcome.DEACTIVATED, service.deleteCompetitor(31L));

        verify(dao).deactivateCompetitor(connection, 31L);
    }

    private void exerciseReasonCrud(String type, long id) throws Exception {
        WinLossReason reason = reason(type, type + " reason");
        when(dao.nextReasonOrder(connection, type)).thenReturn(1);
        when(dao.insertReason(connection, reason)).thenReturn(id);
        when(dao.findReasonById(connection, id)).thenReturn(reason);

        assertSame(reason, service.createReason(reason));
        assertEquals(id, reason.getId());
        assertSame(reason, service.updateReason(reason));
        assertEquals(WinLossService.DeleteOutcome.DELETED, service.deleteReason(id));

        verify(dao).insertReason(connection, reason);
        verify(dao).updateReason(connection, reason);
        verify(dao).deleteReason(connection, id);
    }

    private WinLossReason reason(String type, String text) {
        WinLossReason reason = new WinLossReason();
        reason.setType(type);
        reason.setReasonText(text);
        reason.setDescription("Description");
        reason.setActive(true);
        return reason;
    }

    private Competitor competitor(String name) {
        Competitor competitor = new Competitor();
        competitor.setName(name);
        competitor.setStrengths("Strong support");
        competitor.setWeaknesses("High price");
        competitor.setWebsite("https://example.com");
        competitor.setActive(true);
        return competitor;
    }
}
