package com.crm.dao.audit;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class AuditLogDAO {

    public void insert(
            Long userId,
            String entity,
            String entityId,
            String action,
            String description
    ) throws SQLException {

        insert(
                userId,
                entity,
                entityId,
                action,
                description,
                null,
                null
        );
    }


    public void insert(
            Long userId,
            String entity,
            String entityId,
            String action,
            String description,
            String beforeValue,
            String afterValue
    ) throws SQLException {

        String sql = """
                INSERT INTO audit_logs(
                    user_id,
                    entity_type,
                    entity_id,
                    action,
                    description,
                    before_value,
                    after_value
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            if (userId == null) {
                statement.setNull(
                        1,
                        Types.BIGINT
                );
            } else {
                statement.setLong(
                        1,
                        userId
                );
            }

            statement.setString(2, entity);
            statement.setString(3, entityId);
            statement.setString(4, action);
            statement.setString(5, description);
            statement.setString(6, beforeValue);
            statement.setString(7, afterValue);

            statement.executeUpdate();
        }
    }


    public List<Map<String, Object>> search(
            String entity,
            String action,
            Long userId,
            String from,
            String to,
            int page,
            int size
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT
                    a.id,
                    a.user_id,
                    u.full_name AS user_name,
                    a.entity_type,
                    a.entity_id,
                    a.action,
                    a.description,
                    a.before_value,
                    a.after_value,
                    a.created_at
                FROM audit_logs a
                LEFT JOIN users u
                    ON u.id = a.user_id
                WHERE 1 = 1
                """);

        List<Object> params =
                new ArrayList<>();

        appendFilters(
                sql,
                params,
                entity,
                action,
                userId,
                from,
                to
        );

        sql.append(
                " ORDER BY a.id DESC LIMIT ? OFFSET ?"
        );

        params.add(size);
        params.add(
                (page - 1) * size
        );


        List<Map<String, Object>> result =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (
                    int i = 0;
                    i < params.size();
                    i++
            ) {

                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    Map<String, Object> item =
                            new LinkedHashMap<>();

                    item.put(
                            "id",
                            rs.getLong("id")
                    );

                    item.put(
                            "userId",
                            rs.getObject("user_id")
                    );

                    item.put(
                            "userName",
                            rs.getString("user_name")
                    );

                    item.put(
                            "entity",
                            rs.getString("entity_type")
                    );

                    item.put(
                            "entityId",
                            rs.getString("entity_id")
                    );

                    item.put(
                            "action",
                            rs.getString("action")
                    );

                    item.put(
                            "description",
                            rs.getString("description")
                    );

                    item.put(
                            "beforeValue",
                            rs.getString("before_value")
                    );

                    item.put(
                            "afterValue",
                            rs.getString("after_value")
                    );

                    item.put(
                            "createdAt",
                            rs.getTimestamp(
                                    "created_at"
                            ).toString()
                    );

                    result.add(item);
                }
            }
        }

        return result;
    }


    public long count(
            String entity,
            String action,
            Long userId,
            String from,
            String to
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT COUNT(*)
                FROM audit_logs a
                WHERE 1 = 1
                """);

        List<Object> params =
                new ArrayList<>();

        appendFilters(
                sql,
                params,
                entity,
                action,
                userId,
                from,
                to
        );


        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (
                    int i = 0;
                    i < params.size();
                    i++
            ) {

                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                rs.next();

                return rs.getLong(1);
            }
        }
    }


    private void appendFilters(
            StringBuilder sql,
            List<Object> params,
            String entity,
            String action,
            Long userId,
            String from,
            String to
    ) {

        if (
                entity != null &&
                !entity.isBlank()
        ) {

            sql.append(
                    " AND a.entity_type = ?"
            );

            params.add(entity);
        }


        if (
                action != null &&
                !action.isBlank()
        ) {

            sql.append(
                    " AND a.action = ?"
            );

            params.add(action);
        }


        if (userId != null) {

            sql.append(
                    " AND a.user_id = ?"
            );

            params.add(userId);
        }


        if (
                from != null &&
                !from.isBlank()
        ) {

            sql.append(
                    " AND a.created_at >= ?"
            );

            params.add(
                    Timestamp.valueOf(
                            from +
                            " 00:00:00"
                    )
            );
        }


        if (
                to != null &&
                !to.isBlank()
        ) {

            sql.append(
                    " AND a.created_at <= ?"
            );

            params.add(
                    Timestamp.valueOf(
                            to +
                            " 23:59:59"
                    )
            );
        }
    }
}