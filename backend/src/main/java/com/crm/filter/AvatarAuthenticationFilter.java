package com.crm.filter;

import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebFilter(urlPatterns = {"/profile/avatar", "/profile/avatar/*", "/api/users/me/avatar"})
public class AvatarAuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;
        Object id = null;
        try {
            HttpSession session = req.getSession(false);
            if (session != null) id = session.getAttribute("userId");
        } catch (IllegalStateException ignored) { }
        if (!(id instanceof Number number) || number.longValue() <= 0) {
            res.setStatus(401);
            res.setContentType("application/json;charset=UTF-8");
            new Gson().toJson(Map.of("success", false, "message", "Yêu cầu đăng nhập."), res.getWriter());
            return;
        }
        req.setAttribute("avatarUserId", number.longValue());
        res.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, res);
    }
}
