package com.crm.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/** Protect every cookie-authenticated mutation, including JSON APIs and method overrides. */
public final class CsrfFilter implements Filter {
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;
        var session = req.getSession(false);
        if (SAFE.contains(req.getMethod()) || session == null || session.getAttribute("userId") == null) {
            chain.doFilter(request, response);
            return;
        }
        Object expected = session.getAttribute("htmlFormToken");
        String supplied = req.getHeader("X-CSRF-Token");
        try {
            if (supplied == null) supplied = req.getParameter("csrfToken");
        } catch (IllegalStateException oversizedMultipart) {
            res.sendError(413, "Tệp tải lên vượt quá dung lượng cho phép.");
            return;
        }
        if (!(expected instanceof String token) || supplied == null
                || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            res.sendError(403, "Phiên biểu mẫu không hợp lệ. Vui lòng tải lại trang và thử lại.");
            return;
        }
        chain.doFilter(request, response);
    }
}
