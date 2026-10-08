package com.crm.controller.pipeline;

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

@WebServlet({
        "/api/pipeline-stages",
        "/api/pipeline-stages/*"
})
public class PipelineStageServlet extends HttpServlet {

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
                    "pipeline.read"
            );

            long pipelineId =
                    parsePipelineId(
                            req.getParameter(
                                    "pipelineId"
                            )
                    );

            List<Map<String,Object>> items =
                    new ArrayList<>();

            try (
                    Connection connection =
                            DatabaseConfig.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    """
                                    SELECT
                                        id,
                                        pipeline_id,
                                        name,
                                        order_no,
                                        probability,
                                        exit_condition,
                                        condition_required,
                                        active
                                    FROM pipeline_stages
                                    WHERE pipeline_id = ?
                                    ORDER BY order_no ASC, id ASC
                                    """
                            )
            ) {

                statement.setLong(
                        1,
                        pipelineId
                );

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
                            "Lấy cấu hình pipeline thành công",
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
                null
        );
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

        save(
                req,
                res,
                id
        );
    }


    private void save(
            HttpServletRequest req,
            HttpServletResponse res,
            Long id
    ) throws IOException {

        try {

            require(
                    req,
                    "pipeline.manage"
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

            String name =
                    requiredString(
                            body,
                            "name",
                            "Tên giai đoạn là bắt buộc"
                    );

            int orderNo =
                    requiredInt(
                            body,
                            "orderNo",
                            "Thứ tự là bắt buộc"
                    );

            int probability =
                    requiredInt(
                            body,
                            "probability",
                            "Xác suất là bắt buộc"
                    );

            long pipelineId =
                    body.has("pipelineId")
                    &&
                    !body.get("pipelineId").isJsonNull()
                    ?
                    body.get("pipelineId").getAsLong()
                    :
                    1L;

            String exitCondition =
                    optionalString(
                            body,
                            "exitCondition"
                    );

            boolean conditionRequired =
                    body.has("conditionRequired")
                    &&
                    !body.get("conditionRequired").isJsonNull()
                    &&
                    body.get("conditionRequired").getAsBoolean();

            boolean active =
                    !body.has("active")
                    ||
                    body.get("active").isJsonNull()
                    ||
                    body.get("active").getAsBoolean();


            if (orderNo < 1) {

                throw new IllegalArgumentException(
                        "Thứ tự phải từ 1 trở lên"
                );
            }

            if (
                    probability < 0
                    ||
                    probability > 100
            ) {

                throw new IllegalArgumentException(
                        "Xác suất phải từ 0 đến 100"
                );
            }

            if (
                    conditionRequired
                    &&
                    (
                        exitCondition == null
                        ||
                        exitCondition.isBlank()
                    )
            ) {

                throw new IllegalArgumentException(
                        "Giai đoạn bắt buộc điều kiện phải có nội dung điều kiện"
                );
            }


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
                                            INSERT INTO pipeline_stages(
                                                pipeline_id,
                                                name,
                                                order_no,
                                                probability,
                                                exit_condition,
                                                condition_required,
                                                active
                                            )
                                            VALUES (?, ?, ?, ?, ?, ?, ?)
                                            """,
                                            Statement.RETURN_GENERATED_KEYS
                                    )
                    ) {

                        statement.setLong(
                                1,
                                pipelineId
                        );

                        statement.setString(
                                2,
                                name
                        );

                        statement.setInt(
                                3,
                                orderNo
                        );

                        statement.setInt(
                                4,
                                probability
                        );

                        statement.setString(
                                5,
                                exitCondition
                        );

                        statement.setBoolean(
                                6,
                                conditionRequired
                        );

                        statement.setBoolean(
                                7,
                                active
                        );

                        statement.executeUpdate();

                        try (
                                ResultSet keys =
                                        statement.getGeneratedKeys()
                        ) {

                            if (!keys.next()) {

                                throw new SQLException(
                                        "Không lấy được ID giai đoạn"
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
                                        "Giai đoạn không tồn tại",
                                        null
                                )
                        );

                        return;
                    }

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            """
                                            UPDATE pipeline_stages
                                            SET
                                                pipeline_id = ?,
                                                name = ?,
                                                order_no = ?,
                                                probability = ?,
                                                exit_condition = ?,
                                                condition_required = ?,
                                                active = ?
                                            WHERE id = ?
                                            """
                                    )
                    ) {

                        statement.setLong(
                                1,
                                pipelineId
                        );

                        statement.setString(
                                2,
                                name
                        );

                        statement.setInt(
                                3,
                                orderNo
                        );

                        statement.setInt(
                                4,
                                probability
                        );

                        statement.setString(
                                5,
                                exitCondition
                        );

                        statement.setBoolean(
                                6,
                                conditionRequired
                        );

                        statement.setBoolean(
                                7,
                                active
                        );

                        statement.setLong(
                                8,
                                id
                        );

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
                                "Tạo giai đoạn thành công"
                                :
                                "Cập nhật giai đoạn thành công",
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
                            "Thứ tự giai đoạn đã tồn tại trong pipeline",
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
                                    pipeline_id,
                                    name,
                                    order_no,
                                    probability,
                                    exit_condition,
                                    condition_required,
                                    active
                                FROM pipeline_stages
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

        Map<String,Object> item =
                new LinkedHashMap<>();

        item.put(
                "id",
                rs.getLong("id")
        );

        item.put(
                "pipelineId",
                rs.getLong("pipeline_id")
        );

        item.put(
                "name",
                rs.getString("name")
        );

        item.put(
                "orderNo",
                rs.getInt("order_no")
        );

        item.put(
                "probability",
                rs.getInt("probability")
        );

        item.put(
                "exitCondition",
                rs.getString("exit_condition")
        );

        item.put(
                "conditionRequired",
                rs.getBoolean("condition_required")
        );

        item.put(
                "active",
                rs.getBoolean("active")
        );

        return item;
    }


    private long parsePipelineId(
            String value
    ) {

        if (
                value == null
                ||
                value.isBlank()
        ) {

            return 1L;
        }

        try {

            long result =
                    Long.parseLong(value);

            if (result < 1) {

                throw new Exception();
            }

            return result;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "pipelineId không hợp lệ"
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
                body.get(field).isJsonNull()
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


    private int requiredInt(
            JsonObject body,
            String field,
            String message
    ) {

        if (
                !body.has(field)
                ||
                body.get(field).isJsonNull()
        ) {

            throw new IllegalArgumentException(
                    message
            );
        }

        try {

            return body
                    .get(field)
                    .getAsInt();

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    message
            );
        }
    }


    private String optionalString(
            JsonObject body,
            String field
    ) {

        if (
                !body.has(field)
                ||
                body.get(field).isJsonNull()
        ) {

            return null;
        }

        String value =
                body.get(field)
                        .getAsString()
                        .trim();

        return value.isBlank()
                ?
                null
                :
                value;
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