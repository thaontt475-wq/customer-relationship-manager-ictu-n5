package com.crm.controller.customfields;

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

@WebServlet({
        "/api/custom-fields",
        "/api/custom-fields/*"
})
public class CustomFieldServlet extends HttpServlet {

    private static final Set<String> ENTITY_TYPES =
            Set.of(
                    "CUSTOMER",
                    "OPPORTUNITY"
            );

    private static final Set<String> FIELD_TYPES =
            Set.of(
                    "TEXT",
                    "NUMBER",
                    "DATE",
                    "SELECT"
            );

    private final PermissionService permissions =
            new PermissionService();


    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(req, "customfield.read");

            String entityType =
                    req.getParameter("entityType");

            validateEntity(entityType);

            Boolean active =
                    parseBooleanNullable(
                            req.getParameter("active")
                    );

            StringBuilder sql =
                    new StringBuilder("""
                            SELECT
                                id,
                                entity_type,
                                field_key,
                                label,
                                field_type,
                                options_json,
                                required_field,
                                active
                            FROM custom_fields
                            WHERE entity_type = ?
                            """);

            if (active != null) {

                sql.append(
                        " AND active = ? "
                );
            }

            sql.append(
                    " ORDER BY id ASC "
            );

            List<Map<String,Object>> fields =
                    new ArrayList<>();

            try (
                    Connection connection =
                            DatabaseConfig.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql.toString()
                            )
            ) {

                statement.setString(
                        1,
                        entityType.toUpperCase()
                );

                if (active != null) {

                    statement.setBoolean(
                            2,
                            active
                    );
                }

                try (
                        ResultSet rs =
                                statement.executeQuery()
                ) {

                    while (rs.next()) {

                        fields.add(
                                map(rs)
                        );
                    }
                }
            }

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Lấy custom field thành công",
                            fields
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

        save(req, res, null);
    }


    @Override
    protected void doPut(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        Long id;

        try {

            String path =
                    req.getPathInfo();

            if (
                    path == null
                    ||
                    path.length() <= 1
            ) {

                throw new Exception();
            }

            id =
                    Long.parseLong(
                            path.substring(1)
                    );

        } catch (Exception e) {

            bad(
                    res,
                    "ID không hợp lệ"
            );

            return;
        }

        save(req, res, id);
    }


    private void save(
            HttpServletRequest req,
            HttpServletResponse res,
            Long id
    ) throws IOException {

        try {

            require(
                    req,
                    "customfield.manage"
            );

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

            String entity =
                    requiredString(
                            body,
                            "entityType",
                            "Đối tượng là bắt buộc"
                    )
                    .toUpperCase();

            String key =
                    requiredString(
                            body,
                            "key",
                            "Mã trường là bắt buộc"
                    );

            String label =
                    requiredString(
                            body,
                            "label",
                            "Nhãn hiển thị là bắt buộc"
                    );

            String fieldType =
                    requiredString(
                            body,
                            "fieldType",
                            "Kiểu dữ liệu là bắt buộc"
                    )
                    .toUpperCase();

            validateEntity(entity);

            if (
                    !FIELD_TYPES.contains(
                            fieldType
                    )
            ) {

                throw new IllegalArgumentException(
                        "Kiểu dữ liệu không hợp lệ"
                );
            }

            boolean required =
                    body.has("required")
                    &&
                    !body.get("required").isJsonNull()
                    &&
                    body.get("required").getAsBoolean();

            boolean active =
                    !body.has("active")
                    ||
                    body.get("active").isJsonNull()
                    ||
                    body.get("active").getAsBoolean();


            List<String> options =
                    parseOptions(body);

            if (
                    "SELECT".equals(fieldType)
                    &&
                    options.isEmpty()
            ) {

                throw new IllegalArgumentException(
                        "Trường Select phải có ít nhất một lựa chọn"
                );
            }

            if (
                    !"SELECT".equals(fieldType)
            ) {

                options =
                        new ArrayList<>();
            }

            String optionsJson =
                    options.isEmpty()
                    ?
                    null
                    :
                    JsonUtil.getGson()
                            .toJson(options);


            long savedId;

            try (
                    Connection connection =
                            DatabaseConfig.getConnection()
            ) {

                if (id == null) {

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            INSERT INTO custom_fields(
                                                entity_type,
                                                field_key,
                                                label,
                                                field_type,
                                                options_json,
                                                required_field,
                                                active
                                            )
                                            VALUES (?, ?, ?, ?, ?, ?, ?)
                                            """,
                                            Statement.RETURN_GENERATED_KEYS
                                    )
                    ) {

                        statement.setString(1, entity);
                        statement.setString(2, key);
                        statement.setString(3, label);
                        statement.setString(4, fieldType);
                        statement.setString(5, optionsJson);
                        statement.setBoolean(6, required);
                        statement.setBoolean(7, active);

                        statement.executeUpdate();

                        try (
                                ResultSet keys =
                                        statement.getGeneratedKeys()
                        ) {

                            if (!keys.next()) {

                                throw new SQLException(
                                        "Không lấy được ID custom field"
                                );
                            }

                            savedId =
                                    keys.getLong(1);
                        }
                    }

                } else {

                    if (
                            findById(
                                    connection,
                                    id
                            )
                            == null
                    ) {

                        ResponseUtil.json(
                                res,
                                404,
                                ApiResponse.error(
                                        "Custom field không tồn tại",
                                        null
                                )
                        );

                        return;
                    }

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            UPDATE custom_fields
                                            SET
                                                entity_type = ?,
                                                field_key = ?,
                                                label = ?,
                                                field_type = ?,
                                                options_json = ?,
                                                required_field = ?,
                                                active = ?
                                            WHERE id = ?
                                            """
                                    )
                    ) {

                        statement.setString(1, entity);
                        statement.setString(2, key);
                        statement.setString(3, label);
                        statement.setString(4, fieldType);
                        statement.setString(5, optionsJson);
                        statement.setBoolean(6, required);
                        statement.setBoolean(7, active);
                        statement.setLong(8, id);

                        statement.executeUpdate();
                    }

                    savedId = id;
                }

                Map<String,Object> result =
                        findById(
                                connection,
                                savedId
                        );

                ResponseUtil.json(
                        res,
                        id == null ? 201 : 200,
                        ApiResponse.success(
                                id == null
                                ?
                                "Tạo custom field thành công"
                                :
                                "Cập nhật custom field thành công",
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
                            "Mã trường đã tồn tại trong đối tượng này",
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
            long id
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT
                                    id,
                                    entity_type,
                                    field_key,
                                    label,
                                    field_type,
                                    options_json,
                                    required_field,
                                    active
                                FROM custom_fields
                                WHERE id = ?
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    id
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


    private Map<String,Object> map(
            ResultSet rs
    ) throws SQLException {

        Map<String,Object> field =
                new LinkedHashMap<>();

        field.put(
                "id",
                rs.getLong("id")
        );

        field.put(
                "entityType",
                rs.getString(
                        "entity_type"
                )
        );

        field.put(
                "key",
                rs.getString(
                        "field_key"
                )
        );

        field.put(
                "label",
                rs.getString(
                        "label"
                )
        );

        field.put(
                "fieldType",
                rs.getString(
                        "field_type"
                )
        );

        String optionsJson =
                rs.getString(
                        "options_json"
                );

        List<String> options =
                new ArrayList<>();

        if (
                optionsJson != null
                &&
                !optionsJson.isBlank()
        ) {

            try {

                JsonArray array =
                        JsonParser
                                .parseString(
                                        optionsJson
                                )
                                .getAsJsonArray();

                for (
                        JsonElement item
                        :
                        array
                ) {

                    options.add(
                            item.getAsString()
                    );
                }

            } catch (Exception ignored) {
            }
        }

        field.put(
                "options",
                options
        );

        field.put(
                "required",
                rs.getBoolean(
                        "required_field"
                )
        );

        field.put(
                "active",
                rs.getBoolean(
                        "active"
                )
        );

        return field;
    }


    private List<String> parseOptions(
            JsonObject body
    ) {

        List<String> result =
                new ArrayList<>();

        if (
                !body.has("options")
                ||
                body.get("options").isJsonNull()
        ) {

            return result;
        }

        JsonElement element =
                body.get("options");

        if (element.isJsonArray()) {

            for (
                    JsonElement item
                    :
                    element.getAsJsonArray()
            ) {

                String value =
                        item.getAsString()
                                .trim();

                if (!value.isBlank()) {

                    result.add(value);
                }
            }

        } else {

            String raw =
                    element.getAsString();

            for (
                    String item
                    :
                    raw.split("[,\\n]")
            ) {

                String value =
                        item.trim();

                if (!value.isBlank()) {

                    result.add(value);
                }
            }
        }

        return result;
    }


    private void validateEntity(
            String entity
    ) {

        if (
                entity == null
                ||
                !ENTITY_TYPES.contains(
                        entity.toUpperCase()
                )
        ) {

            throw new IllegalArgumentException(
                    "Đối tượng không hợp lệ"
            );
        }
    }


    private String requiredString(
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
                (Long) session.getAttribute(
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