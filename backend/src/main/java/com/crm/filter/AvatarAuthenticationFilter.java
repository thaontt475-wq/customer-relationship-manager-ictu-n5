package com.crm.filter;

import com.crm.model.User;
import com.crm.util.SessionKey;
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

        Long actorUserId = null;
        try {
            HttpSession session = req.getSession(false);
            if (session != null) {
                Object directUserId = session.getAttribute("userId");
                Object actorIdObj = session.getAttribute("actorUserId");
                Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);

                if (directUserId instanceof Number number && number.longValue() > 0) {
                    actorUserId = number.longValue();
                } else if (actorIdObj instanceof Number number && number.longValue() > 0) {
                    actorUserId = number.longValue();
                } else if (currentUser instanceof User user && user.getId() > 0) {
                    actorUserId = user.getId();
                }
            }
        } catch (IllegalStateException ignored) { }

        if (actorUserId == null || actorUserId <= 0) {
            String contextPath = req.getContextPath();
            String path = req.getServletPath();

            if (path != null && path.startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json;charset=UTF-8");
                new Gson().toJson(Map.of("success", false, "message", "Yêu cầu đăng nhập."), res.getWriter());
            } else {
                res.sendRedirect(contextPath + "/login?expired=1");
            }
            return;
        }

        req.setAttribute("avatarUserId", actorUserId);
        res.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, res);
    }
}
