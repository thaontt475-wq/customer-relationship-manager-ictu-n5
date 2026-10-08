package com.crm.controller.auth;

import com.crm.dto.common.ApiResponse;
import com.crm.service.auth.PasswordResetService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/auth/reset-password")
public class ResetPasswordServlet
        extends HttpServlet {

    private final PasswordResetService service =
            new PasswordResetService();

    private static class RequestBody {
        String token;
        String newPassword;
        String confirmPassword;
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            RequestBody body =
                    JsonUtil.getGson()
                            .fromJson(
                                    request.getReader(),
                                    RequestBody.class
                            );

            if (body == null) {

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

            service.resetPassword(
                    body.token,
                    body.newPassword,
                    body.confirmPassword
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Đặt lại mật khẩu thành công",
                            null
                    )
            );

        } catch (com.google.gson.JsonParseException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("JSON không hợp lệ", null));
        } catch (IllegalArgumentException e) {

            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(
                            e.getMessage(),
                            null
                    )
            );

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
