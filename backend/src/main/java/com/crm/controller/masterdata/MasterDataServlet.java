package com.crm.controller.masterdata;

import com.crm.config.DatabaseConfig;
import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.PermissionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/master-data/*")
public class MasterDataServlet extends HttpServlet {

    private static final Set<String> TYPES =
            Set.of(
                    "industry",
                    "company-size",
                    "lead-source",
                    "activity-type"
            );

    private final PermissionService permissions =
            new PermissionService();


    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(req, "masterdata.read");

            String type = getType(req);

            String keyword =
                    trimToNull(
                            req.getParameter("keyword")
                    );

            Boolean active =
                    parseBooleanNullable(
                            req.getParameter("active")
                    );

            StringBuilder sql =
                    new StringBuilder("""
                            SELECT
                                id,
                                type,
                                code,
                                name,
                                active,
                                display_order
                            FROM master_data
                            WHERE type = ?
                            """);

            List<Object> params =
                    new ArrayList<>();

            params.add(type);

            if (keyword != null) {

                sql.append("""
                         AND (
                             LOWER(code) LIKE LOWER(?)
                             OR LOWER(name) LIKE LOWER(?)
                         )
                        """);

                String like =
                        "%" + keyword + "%";

                params.add(like);
                params.add(like);
            }

            if (active != null) {

                sql.append(
                        " AND active = ? "
                );

                params.add(active);
            }

            sql.append("""
                     ORDER BY
                         display_order ASC,
                         id ASC
                    """);

            List<Map<String,Object>> items =
                    new ArrayList<>();

            try (
                    Connection connection =
                            DatabaseConfig.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql.toString()
                            )
            ) {

                int index = 1;

                for (Object param : params) {

                    if (param instanceof Boolean b) {

                        statement.setBoolean(
                                index++,
                                b
                        );

                    } else {

                        statement.setString(
                                index++,
                                String.valueOf(param)
                        );
                    }
                }

                try (
                        ResultSet rs =
                                statement.executeQuery()
                ) {

                    while (rs.next()) {

                        items.add(
                                map(rs)
                        );
                    }
                }
            }

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Lấy danh mục thành công",
                            items
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            bad(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            error(
                    res,
                    e
            );
        }
    }


    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        save(
                req,
                res,
                false
        );
    }


    @Override
    protected void doPut(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        save(
                req,
                res,
                true
        );
    }


    private void save(
            HttpServletRequest req,
            HttpServletResponse res,
            boolean update
    ) throws IOException {

        try {

            require(
                    req,
                    "masterdata.manage"
            );

            String[] parts =
                    pathParts(req);

            String type =
                    parts[0];

            validateType(type);

            Long id = null;

            if (update) {

                if (parts.length != 2) {

                    throw new IllegalArgumentException(
                            "Thiếu ID"
                    );
                }

                try {

                    id =
                            Long.valueOf(
                                    parts[1]
                            );

                } catch (NumberFormatException e) {

                    throw new IllegalArgumentException(
                            "ID không hợp lệ"
                    );
                }
            }

            JsonObject body =
                    JsonUtil.getGson()
                            .fromJson(
                                    req.getReader(),
                                    JsonObject.class
                            );

            if (body == null) {

                throw new IllegalArgumentException(
                        "Dữ liệu không hợp lệ"
                );
            }

            String code =
                    getRequiredString(
                            body,
                            "code",
                            "Mã là bắt buộc"
                    );

            String name =
                    getRequiredString(
                            body,
                            "name",
                            "Tên hiển thị là bắt buộc"
                    );

            boolean active =
                    !body.has("active")
                    ||
                    body.get("active")
                            .getAsBoolean();

            Integer displayOrder =
                    body.has("displayOrder")
                    &&
                    !body.get("displayOrder")
                            .isJsonNull()
                            ?
                    body.get("displayOrder")
                            .getAsInt()
                    :
                    null;

            long savedId;

            try (
                    Connection connection =
                            DatabaseConfig.getConnection()
            ) {

                if (update) {

                    Map<String,Object> existing =
                            findById(
                                    connection,
                                    type,
                                    id
                            );

                    if (existing == null) {

                        ResponseUtil.json(
                                res,
                                404,
                                ApiResponse.error(
                                        "Danh mục không tồn tại",
                                        null
                                )
                        );

                        return;
                    }

                    int finalOrder =
                            displayOrder != null
                            ?
                            displayOrder
                            :
                            ((Number)
                                    existing.get(
                                            "displayOrder"
                                    )
                            ).intValue();

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            UPDATE master_data
                                            SET
                                                code = ?,
                                                name = ?,
                                                active = ?,
                                                display_order = ?
                                            WHERE id = ?
                                              AND type = ?
                                            """
                                    )
                    ) {

                        statement.setString(
                                1,
                                code
                        );

                        statement.setString(
                                2,
                                name
                        );

                        statement.setBoolean(
                                3,
                                active
                        );

                        statement.setInt(
                                4,
                                finalOrder
                        );

                        statement.setLong(
                                5,
                                id
                        );

                        statement.setString(
                                6,
                                type
                        );

                        statement.executeUpdate();
                    }

                    savedId = id;

                } else {

                    int finalOrder =
                            displayOrder != null
                            ?
                            displayOrder
                            :
                            nextDisplayOrder(
                                    connection,
                                    type
                            );

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            INSERT INTO master_data(
                                                type,
                                                code,
                                                name,
                                                active,
                                                display_order
                                            )
                                            VALUES (?, ?, ?, ?, ?)
                                            """,
                                            Statement.RETURN_GENERATED_KEYS
                                    )
                    ) {

                        statement.setString(
                                1,
                                type
                        );

                        statement.setString(
                                2,
                                code
                        );

                        statement.setString(
                                3,
                                name
                        );

                        statement.setBoolean(
                                4,
                                active
                        );

                        statement.setInt(
                                5,
                                finalOrder
                        );

                        statement.executeUpdate();

                        try (
                                ResultSet keys =
                                        statement.getGeneratedKeys()
                        ) {

                            if (!keys.next()) {

                                throw new SQLException(
                                        "Không lấy được ID"
                                );
                            }

                            savedId =
                                    keys.getLong(1);
                        }
                    }
                }

                Map<String,Object> result =
                        findById(
                                connection,
                                type,
                                savedId
                        );

                ResponseUtil.json(
                        res,
                        update ? 200 : 201,
                        ApiResponse.success(
                                update
                                ?
                                "Cập nhật danh mục thành công"
                                :
                                "Tạo danh mục thành công",
                                result
                        )
                );
            }

        } catch (
                SQLIntegrityConstraintViolationException e
        ) {

            ResponseUtil.json(
                    res,
                    409,
                    ApiResponse.error(
                            "Mã đã tồn tại trong danh mục này",
                            null
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            bad(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            error(
                    res,
                    e
            );
        }
    }


    private Map<String,Object> findById(
            Connection connection,
            String type,
            long id
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT
                                    id,
                                    type,
                                    code,
                                    name,
                                    active,
                                    display_order
                                FROM master_data
                                WHERE id = ?
                                  AND type = ?
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    id
            );

            statement.setString(
                    2,
                    type
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return rs.next()
                        ?
                        map(rs)
                        :
                        null;
            }
        }
    }


    private int nextDisplayOrder(
            Connection connection,
            String type
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT
                                    COALESCE(
                                        MAX(display_order),
                                        0
                                    ) + 1
                                FROM master_data
                                WHERE type = ?
                                """
                        )
        ) {

            statement.setString(
                    1,
                    type
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                rs.next();

                return rs.getInt(1);
            }
        }
    }


    private Map<String,Object> map(
            ResultSet rs
    ) throws SQLException {

        Map<String,Object> item =
                new LinkedHashMap<>();

        item.put(
                "id",
                rs.getLong("id")
        );

        item.put(
                "type",
                rs.getString("type")
        );

        item.put(
                "code",
                rs.getString("code")
        );

        item.put(
                "name",
                rs.getString("name")
        );

        item.put(
                "active",
                rs.getBoolean("active")
        );

        item.put(
                "displayOrder",
                rs.getInt("display_order")
        );

        return item;
    }


    private String[] pathParts(
            HttpServletRequest req
    ) {

        String path =
                req.getPathInfo();

        if (
                path == null
                ||
                path.length() <= 1
        ) {

            throw new IllegalArgumentException(
                    "Thiếu type"
            );
        }

        return path
                .substring(1)
                .split("/");
    }


    private String getType(
            HttpServletRequest req
    ) {

        String type =
                pathParts(req)[0];

        validateType(type);

        return type;
    }


    private void validateType(
            String type
    ) {

        if (!TYPES.contains(type)) {

            throw new IllegalArgumentException(
                    "Master data type không hợp lệ"
            );
        }
    }


    private String getRequiredString(
            JsonObject body,
            String field,
            String message
    ) {

        if (
                !body.has(field)
                ||
                body.get(field)
                        .isJsonNull()
        ) {

            throw new IllegalArgumentException(
                    message
            );
        }

        String value =
                body.get(field)
                        .getAsString()
                        .trim();

        if (value.isBlank()) {

            throw new IllegalArgumentException(
                    message
            );
        }

        return value;
    }


    private Boolean parseBooleanNullable(
            String value
    ) {

        if (
                value == null
                ||
                value.isBlank()
        ) {

            return null;
        }

        if (
                "true".equalsIgnoreCase(value)
                ||
                "1".equals(value)
        ) {

            return true;
        }

        if (
                "false".equalsIgnoreCase(value)
                ||
                "0".equals(value)
        ) {

            return false;
        }

        throw new IllegalArgumentException(
                "active không hợp lệ"
        );
    }


    private String trimToNull(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ?
                null
                :
                trimmed;
    }


    private void require(
            HttpServletRequest req,
            String permission
    ) throws Exception {

        HttpSession session =
                req.getSession(false);

        if (
                session == null
                ||
                session.getAttribute(
                        "userId"
                ) == null
        ) {

            throw new SecurityException();
        }

        long userId =
                (Long) session
                        .getAttribute(
                                "userId"
                        );

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


    private void bad(
            HttpServletResponse res,
            String message
    ) throws IOException {

        ResponseUtil.json(
                res,
                400,
                ApiResponse.error(
                        message,
                        null
                )
        );
    }


    private void error(
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