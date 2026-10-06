package com.crm.controller.products;

import com.crm.config.DatabaseConfig;
import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.PermissionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import com.google.gson.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/price-books")
public class PriceBookServlet
        extends HttpServlet {

    private final PermissionService permissions =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(
                    req,
                    "pricebook.read"
            );

            List<Map<String,Object>> items =
                    new ArrayList<>();

            String sql = """
                    SELECT
                        id,
                        name,
                        effective_from,
                        active
                    FROM price_books
                    ORDER BY id DESC
                    """;

            try (
                    Connection connection =
                            DatabaseConfig.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    Map<String,Object> item =
                            new LinkedHashMap<>();

                    item.put(
                            "id",
                            rs.getLong("id")
                    );

                    item.put(
                            "name",
                            rs.getString("name")
                    );

                    item.put(
                            "effectiveFrom",
                            rs.getDate(
                                    "effective_from"
                            ).toString()
                    );

                    item.put(
                            "active",
                            rs.getBoolean("active")
                    );

                    items.add(item);
                }
            }

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Lấy bảng giá thành công",
                            items
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (Exception e) {

            serverError(res,e);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(
                    req,
                    "pricebook.create"
            );

            JsonObject body =
                    JsonUtil.getGson()
                            .fromJson(
                                    req.getReader(),
                                    JsonObject.class
                            );

            String name =
                    body.get("name")
                            .getAsString()
                            .trim();

            java.sql.Date effectiveFrom =
                    java.sql.Date.valueOf(
                            body.get(
                                    "effectiveFrom"
                            ).getAsString()
                    );

            JsonArray lines =
                    body.getAsJsonArray(
                            "lines"
                    );

            if (
                    name.isBlank() ||
                    lines == null ||
                    lines.isEmpty()
            ) {
                throw new IllegalArgumentException(
                        "Tên và lines là bắt buộc"
                );
            }

            long priceBookId;

            try (
                    Connection connection =
                            DatabaseConfig.getConnection()
            ) {

                connection.setAutoCommit(false);

                try {

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            INSERT INTO price_books(
                                                name,
                                                effective_from
                                            )
                                            VALUES (?, ?)
                                            """,
                                            Statement.RETURN_GENERATED_KEYS
                                    )
                    ) {

                        statement.setString(
                                1,
                                name
                        );

                        statement.setDate(
                                2,
                                effectiveFrom
                        );

                        statement.executeUpdate();

                        try (
                                ResultSet keys =
                                        statement.getGeneratedKeys()
                        ) {
                            keys.next();

                            priceBookId =
                                    keys.getLong(1);
                        }
                    }

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            INSERT INTO price_book_lines(
                                                price_book_id,
                                                product_id,
                                                price
                                            )
                                            VALUES (?, ?, ?)
                                            """
                                    )
                    ) {

                        for (
                                JsonElement element :
                                lines
                        ) {

                            JsonObject line =
                                    element.getAsJsonObject();

                            double price =
                                    line.get("price")
                                            .getAsDouble();

                            if (price < 0) {
                                throw new IllegalArgumentException(
                                        "Giá không được âm"
                                );
                            }

                            statement.setLong(
                                    1,
                                    priceBookId
                            );

                            statement.setLong(
                                    2,
                                    line.get(
                                            "productId"
                                    ).getAsLong()
                            );

                            statement.setDouble(
                                    3,
                                    price
                            );

                            statement.addBatch();
                        }

                        statement.executeBatch();
                    }

                    connection.commit();

                } catch (Exception e) {

                    connection.rollback();
                    throw e;
                }
            }

            Map<String,Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "id",
                    priceBookId
            );

            data.put(
                    "name",
                    name
            );

            data.put(
                    "effectiveFrom",
                    effectiveFrom.toString()
            );

            ResponseUtil.json(
                    res,
                    201,
                    ApiResponse.success(
                            "Tạo bảng giá thành công",
                            data
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            ResponseUtil.json(
                    res,
                    400,
                    ApiResponse.error(
                            e.getMessage(),
                            null
                    )
            );

        } catch (Exception e) {

            serverError(res,e);
        }
    }

    private void require(
            HttpServletRequest req,
            String permission
    ) throws Exception {

        long userId =
                (Long) req
                        .getSession(false)
                        .getAttribute("userId");

        if (
                !permissions.hasPermission(
                        userId,
                        permission
                )
        ) {
            throw new SecurityException();
        }
    }

    private void forbidden(
            HttpServletResponse res
    ) throws IOException {

        ResponseUtil.json(
                res,
                403,
                ApiResponse.error(
                        "Không có quyền",
                        null
                )
        );
    }

    private void serverError(
            HttpServletResponse res,
            Exception e
    ) throws IOException {

        e.printStackTrace();

        ResponseUtil.json(
                res,
                500,
                ApiResponse.error(
                        "Lỗi hệ thống",
                        null
                )
        );
    }
}