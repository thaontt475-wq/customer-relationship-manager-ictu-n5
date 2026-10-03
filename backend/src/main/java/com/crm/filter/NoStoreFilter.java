package com.crm.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Prevents browsers from caching authenticated HTML/JSON so that Back after logout
 * re-requests the page from the server (and is redirected to /login) instead of restoring it.
 */
@WebFilter(urlPatterns = "/*", dispatcherTypes = DispatcherType.REQUEST)
public final class NoStoreFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean isStatic = path.startsWith("/css/") || path.startsWith("/images/") || path.startsWith("/fonts/");
        if (!isStatic) {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
        }
        chain.doFilter(request, response);
    }
}
