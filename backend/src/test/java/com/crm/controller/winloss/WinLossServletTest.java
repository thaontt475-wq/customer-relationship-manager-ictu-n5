package com.crm.controller.winloss;

import com.crm.model.Competitor;
import com.crm.model.WinLossReason;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.crm.service.winloss.WinLossService;
import com.crm.util.SessionKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WinLossServletTest {
    private WinLossService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter output;
    private WinLossServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        service = mock(WinLossService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        servlet = new WinLossServlet(service);
    }

    @Test
    void mutationRequiresAuthentication() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).setStatus(401);
        verifyNoInteractions(service);
    }

    @Test
    void getReasonsReturnsSeparateWinAndLossArrays() throws Exception {
        authenticate("Sales Rep");
        when(request.getServletPath()).thenReturn("/api/winloss/reasons");
        WinLossReason win = new WinLossReason();
        win.setId(11L);
        win.setType("WIN");
        WinLossReason loss = new WinLossReason();
        loss.setId(12L);
        loss.setType("LOSS");
        when(service.getReasons(false)).thenReturn(
                new WinLossService.ReasonsResult(List.of(win), List.of(loss)));

        servlet.doGet(request, response);

        JsonObject envelope = JsonParser.parseString(output.toString()).getAsJsonObject();
        assertTrue(envelope.get("success").getAsBoolean());
        JsonObject data = envelope.getAsJsonObject("data");
        assertEquals(1, data.getAsJsonArray("winReasons").size());
        assertEquals(1, data.getAsJsonArray("lossReasons").size());
        assertEquals("WIN", data.getAsJsonArray("winReasons").get(0)
                .getAsJsonObject().get("type").getAsString());
        assertEquals("LOSS", data.getAsJsonArray("lossReasons").get(0)
                .getAsJsonObject().get("type").getAsString());
        verify(response).setStatus(200);
        verify(service, never()).getCompetitors(anyBoolean());
    }

    @Test
    void mutationRejectsNonAdmin() throws Exception {
        authenticate("Sales Rep");

        servlet.doPost(request, response);

        verify(response).setStatus(403);
        verifyNoInteractions(service);
    }

    @Test
    void adminCanCreateCompetitorWithStandardEnvelope() throws Exception {
        authenticate("Admin");
        when(request.getServletPath()).thenReturn("/api/winloss/competitors");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("""
                {"name":"Apex CRM","strengths":"Support","weaknesses":"Price",
                 "website":"https://example.com"}
                """)));
        Competitor created = new Competitor();
        created.setId(1L);
        created.setName("Apex CRM");
        when(service.createCompetitor(any(Competitor.class))).thenReturn(created);

        servlet.doPost(request, response);

        verify(response).setStatus(201);
        assertTrue(output.toString().contains("\"success\":true"));
        assertTrue(output.toString().contains("\"message\""));
        assertTrue(output.toString().contains("\"data\""));
    }

    private void authenticate(String role) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(99L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of(role));
    }
}
