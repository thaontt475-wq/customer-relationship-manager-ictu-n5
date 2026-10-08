package com.crm.controller.profile;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.profile.ProfileUpdateRequest;
import com.crm.service.profile.ProfileService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonParseException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/profile")
public class ProfileServlet extends HttpServlet {

    private final ProfileService service =
            new ProfileService();


    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            long userId =
                    currentUser(request);

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy profile thành công",
                            service.get(userId)
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();
            serverError(response, e.getMessage());
        }
    }


    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            ProfileUpdateRequest body =
                    JsonUtil.getGson()
                            .fromJson(
                                    request.getReader(),
                                    ProfileUpdateRequest.class
                            );


            if (body == null) {

                throw new IllegalArgumentException(
                        "Dữ liệu không hợp lệ"
                );
            }


            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Cập nhật profile thành công",
                            service.update(
                                    currentUser(request),
                                    body.getFullName(),
                                    body.getPhone(),
                                    body.getEmailSignature()
                            )
                    )
            );


        } catch (JsonParseException e) {

            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(
                            "JSON không hợp lệ",
                            null
                    )
            );

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

            e.printStackTrace();
            serverError(response, e.getMessage());
        }
    }


    private long currentUser(
            HttpServletRequest request
    ) {

        return ((Number) request
                .getSession(false)
                .getAttribute("userId"))
                .longValue();
    }


    private void serverError(
            HttpServletResponse response,
            String details
    ) throws IOException {

        ResponseUtil.json(
                response,
                500,
                ApiResponse.error(
                        details != null && !details.isBlank() ? "Lỗi hệ thống: " + details : "Lỗi hệ thống",
                        null
                )
        );
    }
}