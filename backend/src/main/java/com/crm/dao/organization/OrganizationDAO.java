package com.crm.dao.organization;

import com.crm.config.DatabaseConfig;
import com.crm.dto.organization.OrganizationUnitRequest;

import java.sql.*;
import java.util.*;

public class OrganizationDAO {

    public List<Map<String, Object>> findAll(
            Long parentId,
            Boolean active
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    ou.id,
                    ou.code,
                    ou.name,
                    ou.type,
                    ou.parent_id,
                    ou.manager_id,
                    u.full_name AS manager_name,
                    ou.description,
                    ou.active
                FROM organization_units ou
                LEFT JOIN users u
                    ON u.id = ou.manager_id
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (parentId != null) {
            sql.append(" AND ou.parent_id = ?");
            params.add(parentId);
        }

        if (active != null) {
            sql.append(" AND ou.active = ?");
            params.add(active);
        }

        sql.append(" ORDER BY ou.id");

        List<Map<String, Object>> items =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0; i < params.size(); i++) {
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
                    items.add(map(rs));
                }
            }
        }

        return items;
    }


    public Map<String, Object> findById(
            long id
    ) throws SQLException {

        String sql = """
                SELECT
                    ou.id,
                    ou.code,
                    ou.name,
                    ou.type,
                    ou.parent_id,
                    ou.manager_id,
                    u.full_name AS manager_name,
                    ou.description,
                    ou.active
                FROM organization_units ou
                LEFT JOIN users u
                    ON u.id = ou.manager_id
                WHERE ou.id = ?
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return rs.next()
                        ? map(rs)
                        : null;
            }
        }
    }


    public long create(
            OrganizationUnitRequest request
    ) throws SQLException {

        String sql = """
                INSERT INTO organization_units(
                    code,
                    name,
                    type,
                    parent_id,
                    manager_id,
                    description,
                    active
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(
                    1,
                    request.getCode()
            );

            statement.setString(
                    2,
                    request.getName()
            );

            statement.setString(
                    3,
                    request.getType()
            );

            setNullableLong(
                    statement,
                    4,
                    request.getParentId()
            );

            setNullableLong(
                    statement,
                    5,
                    request.getManagerId()
            );

            statement.setString(
                    6,
                    request.getDescription()
            );

            statement.setBoolean(
                    7,
                    request.getActive() == null
                            || request.getActive()
            );

            statement.executeUpdate();

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }


    public void update(
            long id,
            OrganizationUnitRequest request
    ) throws SQLException {

        String sql = """
                UPDATE organization_units
                SET
                    code = ?,
                    name = ?,
                    type = ?,
                    parent_id = ?,
                    manager_id = ?,
                    description = ?,
                    active = ?
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    request.getCode()
            );

            statement.setString(
                    2,
                    request.getName()
            );

            statement.setString(
                    3,
                    request.getType()
            );

            setNullableLong(
                    statement,
                    4,
                    request.getParentId()
            );

            setNullableLong(
                    statement,
                    5,
                    request.getManagerId()
            );

            statement.setString(
                    6,
                    request.getDescription()
            );

            statement.setBoolean(
                    7,
                    request.getActive() == null
                            || request.getActive()
            );

            statement.setLong(
                    8,
                    id
            );

            statement.executeUpdate();
        }
    }


    public boolean existsCode(
            String code,
            Long excludeId
    ) throws SQLException {

        String sql = excludeId == null
                ? """
                  SELECT 1
                  FROM organization_units
                  WHERE LOWER(code) = LOWER(?)
                  LIMIT 1
                  """
                : """
                  SELECT 1
                  FROM organization_units
                  WHERE LOWER(code) = LOWER(?)
                  AND id <> ?
                  LIMIT 1
                  """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    code
            );

            if (excludeId != null) {
                statement.setLong(
                        2,
                        excludeId
                );
            }

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {
                return rs.next();
            }
        }
    }


    public boolean userExists(
            long userId
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM users
                WHERE id = ?
                AND status <> 'DELETED'
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    userId
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {
                return rs.next();
            }
        }
    }


    private void setNullableLong(
            PreparedStatement statement,
            int index,
            Long value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(
                    index,
                    Types.BIGINT
            );
        } else {
            statement.setLong(
                    index,
                    value
            );
        }
    }


    private Map<String, Object> map(
            ResultSet rs
    ) throws SQLException {

        Map<String, Object> item =
                new LinkedHashMap<>();

        item.put(
                "id",
                rs.getLong("id")
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
                "type",
                rs.getString("type")
        );

        item.put(
                "parentId",
                rs.getObject("parent_id")
        );

        item.put(
                "managerId",
                rs.getObject("manager_id")
        );

        item.put(
                "manager",
                rs.getString("manager_name")
        );

        item.put(
                "description",
                rs.getString("description")
        );

        item.put(
                "active",
                rs.getBoolean("active")
        );

        return item;
    }
}