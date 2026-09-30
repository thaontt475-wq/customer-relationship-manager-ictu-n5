package com.crm.filter;

import com.crm.util.SessionKey;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {"/permissions", "/permissions/*", "/api/permissions/*"})
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session;
        try {
            session = httpRequest.getSession(false);
        } catch (IllegalStateException e) {
            rejectUnauthenticated(httpRequest, httpResponse);
            return;
        }

        if (session == null) {
            rejectUnauthenticated(httpRequest, httpResponse);
            return;
        }

        Object currentUser;
        try {
            currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        } catch (IllegalStateException e) {
            rejectUnauthenticated(httpRequest, httpResponse);
            return;
        }

        if (currentUser == null) {
            rejectUnauthenticated(httpRequest, httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.sendRedirect(request.getContextPath() + "/login?expired=1");
        }
    }
}
