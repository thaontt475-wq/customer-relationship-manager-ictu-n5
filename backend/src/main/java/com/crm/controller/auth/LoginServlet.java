package com.crm.controller.auth;

import com.crm.dto.auth.LoginRequest;
import com.crm.dto.common.ApiResponse;
import com.crm.model.users.User;
import com.crm.service.auth.AuthService;
import com.crm.service.auth.LoginResult;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService =
            new AuthService();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            LoginRequest loginRequest =
                    JsonUtil.getGson().fromJson(
                            request.getReader(),
                            LoginRequest.class
                    );

            if (loginRequest == null) {
                ResponseUtil.json(
                        response,
                        400,
                        ApiResponse.error(
                                "Dữ liệu không hợp lệ",
                                null
                        )
                );
                return;
            }

            LoginResult result =
                    authService.login(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    );

            if (
                    result.getStatus() ==
                    LoginResult.Status.INVALID
            ) {

                Map<String, Object> data =
                        new LinkedHashMap<>();

                data.put(
                        "remainingAttempts",
                        result.getRemainingAttempts()
                );

                ResponseUtil.json(
                        response,
                        401,
                        ApiResponse.error(
                                "Sai email hoặc mật khẩu",
                                data
                        )
                );

                return;
            }

            if (
                    result.getStatus() ==
                    LoginResult.Status.LOCKED
            ) {

                Map<String, Object> data =
                        new LinkedHashMap<>();

                data.put(
                        "lockedUntil",
                        result.getLockedUntil().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime().toString()
                );

                ResponseUtil.json(
                        response,
                        423,
                        ApiResponse.error(
                                "Tài khoản tạm khóa trong 15 phút",
                                data
                        )
                );

                return;
            }

            User user =
                    result.getUser();

            HttpSession session =
                    request.getSession(true);

            request.changeSessionId();
            session.setAttribute("sessionVersion", user.getSessionVersion());

            session.setMaxInactiveInterval(
                    30 * 60
            );

            session.setAttribute(
                    "userId",
                    user.getId()
            );

            session.setAttribute(
                    "roles",
                    user.getRoles()
            );

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put("id", user.getId());
            data.put(
                    "fullName",
                    user.getFullName()
            );
            data.put(
                    "email",
                    user.getEmail()
            );
            data.put(
                    "roles",
                    user.getRoles()
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Đăng nhập thành công",
                            data
                    )
            );

        } catch (com.google.gson.JsonParseException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("JSON không hợp lệ", null));
        } catch (Exception e) {

            System.err.println("Authentication request failed: " + e.getClass().getSimpleName());

            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error(
                            "Lỗi hệ thống",
                            null
                    )
            );
        }
    }
}

