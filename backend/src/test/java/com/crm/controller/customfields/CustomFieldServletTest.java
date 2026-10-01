package com.crm.controller.customfields;

import com.crm.model.CustomField;
import com.crm.service.customfields.CustomFieldService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomFieldServletTest {
    private CustomFieldService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter body;
    private CustomFieldServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        service = mock(CustomFieldService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        servlet = new CustomFieldServlet(service);
    }

    @Test
    void definitionCreateRequiresLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).setStatus(401);
        verifyNoInteractions(service);
    }

    @Test
    void definitionCreateRejectsAuthenticatedNonAdmin() throws Exception {
        authenticateWithRole("SALES");

        servlet.doPost(request, response);

        verify(response).setStatus(403);
        verifyNoInteractions(service);
    }

    @Test
    void definitionCreateAllowsAdminAndKeepsEnvelope() throws Exception {
        authenticateWithRole("ADMIN");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("""
                {"entityType":"CUSTOMER","fieldName":"tax_code","fieldLabel":"Tax code",
                 "fieldType":"TEXT","isRequired":false,"options":[],"active":true,
                 "inForm":true,"inFilter":true,"inExport":true}
                """)));
        CustomField created = new CustomField();
        created.setId(1L);
        when(service.createDefinition(any(CustomField.class))).thenReturn(created);

        servlet.doPost(request, response);

        verify(response).setStatus(201);
        assertTrue(body.toString().contains("\"success\":true"));
        assertTrue(body.toString().contains("\"data\""));
    }

    private void authenticateWithRole(String role) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(10L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of(role));
    }
}
