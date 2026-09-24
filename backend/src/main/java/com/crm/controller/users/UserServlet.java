package com.crm.controller.users;

import com.crm.dto.ApiResponse;
import com.crm.dto.users.UserRequest;
import com.crm.dto.users.UserResponse;
import com.crm.service.users.UserService;
import com.crm.util.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {

    private final UserService userService =
            new UserService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            String path = request.getPathInfo();

            if (path != null &&
                    !path.equals("/") &&
                    !path.isBlank()) {

                long id =
                        Long.parseLong(
                                path.substring(1)
                        );

                JsonUtil.write(
                        response,
                        200,
                        ApiResponse.success(
                                "Lấy User thành công",
                                userService.get(id)
                        )
                );

                return;
            }

            String keyword =
                    request.getParameter("keyword");

            Boolean active = null;

            String activeParam =
                    request.getParameter("active");

            if (activeParam != null &&
                    !activeParam.isBlank()) {

                active =
                        Boolean.parseBoolean(
                                activeParam
                        );
            }

            int page =
                    parsePositiveInt(
                            request.getParameter("page"),
                            1
                    );

            int size =
                    parsePositiveInt(
                            request.getParameter("size"),
                            20
                    );

            List<UserResponse> items =
                    userService.search(
                            keyword,
                            active,
                            page,
                            size
                    );

            int totalItems =
                    userService.count(
                            keyword,
                            active
                    );

            int totalPages =
                    (int) Math.ceil(
                            (double) totalItems / size
                    );

            Map<String, Object> data =
                    new HashMap<>();

            data.put("items", items);
            data.put("page", page);
            data.put("size", size);
            data.put("totalItems", totalItems);
            data.put("totalPages", totalPages);

            JsonUtil.write(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy danh sách User thành công",
                            data
                    )
            );

        } catch (IllegalArgumentException e) {

            JsonUtil.write(
                    response,
                    400,
                    ApiResponse.error(e.getMessage())
            );

        } catch (Exception e) {

            e.printStackTrace();

            JsonUtil.write(
                    response,
                    500,
                    ApiResponse.error("Lỗi server")
            );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            UserRequest body =
                    JsonUtil.gson().fromJson(
                            request.getReader(),
                            UserRequest.class
                    );

            UserResponse created =
                    userService.create(body);

            JsonUtil.write(
                    response,
                    201,
                    ApiResponse.success(
                            "Tạo User thành công",
                            created
                    )
            );

        } catch (IllegalArgumentException e) {

            JsonUtil.write(
                    response,
                    400,
                    ApiResponse.error(e.getMessage())
            );

        } catch (Exception e) {

            e.printStackTrace();

            JsonUtil.write(
                    response,
                    500,
                    ApiResponse.error("Lỗi server")
            );
        }
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            String path = request.getPathInfo();

            if (path == null ||
                    path.equals("/")) {

                throw new IllegalArgumentException(
                        "Thiếu User ID"
                );
            }

            long id =
                    Long.parseLong(
                            path.substring(1)
                    );

            UserRequest body =
                    JsonUtil.gson().fromJson(
                            request.getReader(),
                            UserRequest.class
                    );

            JsonUtil.write(
                    response,
                    200,
                    ApiResponse.success(
                            "Cập nhật User thành công",
                            userService.update(id, body)
                    )
            );

        } catch (IllegalArgumentException e) {

            JsonUtil.write(
                    response,
                    400,
                    ApiResponse.error(e.getMessage())
            );

        } catch (Exception e) {

            e.printStackTrace();

            JsonUtil.write(
                    response,
                    500,
                    ApiResponse.error("Lỗi server")
            );
        }
    }

    private int parsePositiveInt(
            String value,
            int defaultValue
    ) {
        try {
            int result =
                    Integer.parseInt(value);

            return result > 0
                    ? result
                    : defaultValue;

        } catch (Exception e) {
            return defaultValue;
        }
    }
}
