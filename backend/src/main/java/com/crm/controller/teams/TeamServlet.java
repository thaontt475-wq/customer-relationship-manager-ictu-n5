package com.crm.controller.teams;

import com.crm.service.teams.TeamService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/teams")
public class TeamServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
            Logger.getLogger(TeamServlet.class.getName());

    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    private final TeamService teamService =
            new TeamService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        HttpSession session;

        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            writeJson(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Yêu cầu đăng nhập",
                    null);
            return;
        }

        if (session == null
                || session.getAttribute(SessionKey.CURRENT_USER) == null) {

            writeJson(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Yêu cầu đăng nhập",
                    null);
            return;
        }

        try {
            writeJson(response,
                    HttpServletResponse.SC_OK,
                    true,
                    "Lấy danh sách nhóm kinh doanh thành công",
                    teamService.findAllTeams());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Unable to load teams", e);

            writeJson(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Lỗi hệ thống khi lấy danh sách nhóm",
                    null);
        }
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );
        response.setStatus(status);

        GSON.toJson(
                new ApiResponse(
                        success,
                        message,
                        data
                ),
                response.getWriter()
        );
    }

    private record ApiResponse(
            boolean success,
            String message,
            Object data) {
    }
}