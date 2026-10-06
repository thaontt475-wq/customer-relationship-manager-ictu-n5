package com.crm.controller.profile;

import com.crm.dto.common.ApiResponse;
import com.crm.service.profile.AvatarService;
import com.crm.util.ResponseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/profile/avatar")
@MultipartConfig(
        maxFileSize = 2097152,
        maxRequestSize = 2200000
)
public class AvatarServlet
        extends HttpServlet {

    private final AvatarService service =
            new AvatarService();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            long userId =
                    (Long) request
                            .getSession(false)
                            .getAttribute("userId");

            Part file =
                    request.getPart(
                            "file"
                    );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Upload avatar thành công",
                            service.upload(
                                    userId,
                                    file
                            )
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

        } catch (IllegalStateException e) {

            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(
                            "Ảnh hoặc request vượt quá dung lượng cho phép",
                            null
                    )
            );

        } catch (ServletException e) {

            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(
                            "Dữ liệu upload không hợp lệ",
                            null
                    )
            );

        } catch (Exception e) {

            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error(
                            "Lỗi upload avatar",
                            null
                    )
            );
        }
    }
}
