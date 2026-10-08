package com.crm.dao.teams;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class TeamDAO {

    public List<Map<String, Object>> findAll(
            String keyword,
            Boolean active
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT id, name, active
                FROM teams
                WHERE 1 = 1
                """);

        List<Object> params =
                new ArrayList<>();

        if (
                keyword != null &&
                !keyword.isBlank()
        ) {
            sql.append(
                    " AND LOWER(name) LIKE ?"
            );

            params.add(
                    "%" +
                    keyword.trim().toLowerCase() +
                    "%"
            );
        }

        if (active != null) {
            sql.append(
                    " AND active = ?"
            );
            params.add(active);
        }

        sql.append(" ORDER BY name");

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

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> team =
                            new LinkedHashMap<>();

                    team.put(
                            "id",
                            rs.getLong("id")
                    );

                    team.put(
                            "name",
                            rs.getString("name")
                    );

                    team.put(
                            "active",
                            rs.getBoolean("active")
                    );

                    result.add(team);
                }
            }
        }

        return result;
    }

    public boolean existsActive(
            long id
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM teams
                WHERE id = ?
                  AND active = TRUE
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }
}