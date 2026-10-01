package com.crm.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** JSPs are rendered through controllers so direct URLs cannot bypass authorization. */
@WebFilter(urlPatterns = "/jsp/*", dispatcherTypes = DispatcherType.REQUEST)
public final class ViewAccessFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException {
        ((HttpServletResponse) response).sendError(404);
    }
}
