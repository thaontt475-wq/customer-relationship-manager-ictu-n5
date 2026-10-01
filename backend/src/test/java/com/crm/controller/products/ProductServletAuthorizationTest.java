package com.crm.controller.products;

import com.crm.service.products.ProductService;
import com.crm.util.SessionKey;
import jakarta.servlet.http.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.util.List;
import static org.mockito.Mockito.*;

class ProductServletAuthorizationTest {
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE", "POST_PUT", "POST_DELETE"})
    void salesRepCannotMutateThroughApiOrMethodOverride(String method) throws Exception {
        var service = mock(ProductService.class);
        var servlet = new ProductServlet(service);
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(10L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("Sales Rep"));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        if (method.startsWith("POST_")) when(request.getParameter("_method")).thenReturn(method.substring(5));
        switch (method) {
            case "PUT" -> servlet.doPut(request, response);
            case "DELETE" -> servlet.doDelete(request, response);
            default -> servlet.doPost(request, response);
        }
        verify(response).setStatus(403);
        verifyNoInteractions(service);
    }
}
