package com.crm.dao.products;

import com.crm.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class ProductDAO {

    public List<Map<String,Object>> findAll(
            String keyword,
            Boolean active
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT
                    id,
                    code,
                    name,
                    type,
                    unit,
                    list_price,
                    floor_price,
                    cost_price,
                    active
                FROM products
                WHERE 1 = 1
                """);

        List<Object> params =
                new ArrayList<>();

        if (
                keyword != null &&
                !keyword.isBlank()
        ) {

            sql.append("""
                 AND (
                     LOWER(code) LIKE ?
                     OR LOWER(name) LIKE ?
                 )
                 """);

            String value =
                    "%" +
                    keyword.toLowerCase() +
                    "%";

            params.add(value);
            params.add(value);
        }

        if (active != null) {

            sql.append(
                    " AND active = ?"
            );

            params.add(active);
        }

        sql.append(
                " ORDER BY id DESC"
        );

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

                    items.add(
                            map(rs)
                    );
                }
            }
        }

        return items;
    }


    public Map<String,Object> findById(
            long id
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    code,
                    name,
                    type,
                    unit,
                    list_price,
                    floor_price,
                    cost_price,
                    active
                FROM products
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
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
                        ? map(rs)
                        : null;
            }
        }
    }


    public long create(
            String code,
            String name,
            String type,
            String unit,
            BigDecimal listPrice,
            BigDecimal floorPrice,
            BigDecimal costPrice,
            boolean active
    ) throws SQLException {

        String sql = """
                INSERT INTO products(
                    code,
                    name,
                    type,
                    unit,
                    list_price,
                    floor_price,
                    cost_price,
                    active
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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

            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, type);
            statement.setString(4, unit);
            statement.setBigDecimal(5, listPrice);
            statement.setBigDecimal(6, floorPrice);
            statement.setBigDecimal(7, costPrice);
            statement.setBoolean(8, active);

            statement.executeUpdate();

            try (
                    ResultSet rs =
                            statement.getGeneratedKeys()
            ) {

                rs.next();

                return rs.getLong(1);
            }
        }
    }


    public void update(
            long id,
            String code,
            String name,
            String type,
            String unit,
            BigDecimal listPrice,
            BigDecimal floorPrice,
            BigDecimal costPrice,
            boolean active
    ) throws SQLException {

        String sql = """
                UPDATE products
                SET
                    code = ?,
                    name = ?,
                    type = ?,
                    unit = ?,
                    list_price = ?,
                    floor_price = ?,
                    cost_price = ?,
                    active = ?
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, type);
            statement.setString(4, unit);
            statement.setBigDecimal(5, listPrice);
            statement.setBigDecimal(6, floorPrice);
            statement.setBigDecimal(7, costPrice);
            statement.setBoolean(8, active);
            statement.setLong(9, id);

            statement.executeUpdate();
        }
    }


    public void deactivate(
            long id
    ) throws SQLException {

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                "UPDATE products SET active = FALSE WHERE id = ?"
                        )
        ) {

            statement.setLong(
                    1,
                    id
            );

            statement.executeUpdate();
        }
    }


    public boolean isReferenced(
            long id
    ) throws SQLException {

        String sql = """
                SELECT (
                    EXISTS(SELECT 1 FROM price_book_lines WHERE product_id = ?)
                    OR
                    EXISTS(SELECT 1 FROM quote_items WHERE product_id = ?)
                )
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);
            statement.setLong(2, id);

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                rs.next();

                return rs.getBoolean(1);
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
                "unit",
                rs.getString("unit")
        );

        item.put(
                "listPrice",
                rs.getBigDecimal(
                        "list_price"
                )
        );

        item.put(
                "floorPrice",
                rs.getBigDecimal(
                        "floor_price"
                )
        );

        item.put(
                "costPrice",
                rs.getBigDecimal(
                        "cost_price"
                )
        );

        item.put(
                "active",
                rs.getBoolean(
                        "active"
                )
        );

        return item;
    }
}