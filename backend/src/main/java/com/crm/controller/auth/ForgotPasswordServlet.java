package com.crm.controller.auth;

import com.crm.dto.common.ApiResponse;
import com.crm.service.auth.PasswordResetService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/auth/forgot-password")
public class ForgotPasswordServlet
        extends HttpServlet {

    private final PasswordResetService service =
            new PasswordResetService();

    private static class RequestBody {
        String email;
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

            if (
                    body == null ||
                    body.email == null ||
                    body.email.isBlank()
            ) {

                ResponseUtil.json(
                        response,
                        400,
                        ApiResponse.error(
                                "Email là bắt buộc",
                                null
                        )
                );

                return;
            }

            service.forgotPassword(
                    body.email
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi",
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

            /*
             * Vẫn trả generic để không làm lộ
             * email nào tồn tại trong hệ thống.
             */

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi",
                            null
                    )
            );
        }
    }
}
